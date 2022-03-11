package cn.miozus.gulimall.seckill.aspect;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import cn.miozus.gulimall.common.annotation.Idempotent;
import cn.miozus.gulimall.common.annotation.PutRedis;
import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.seckill.feign.ProductFeignService;
import cn.miozus.gulimall.seckill.to.SeckillSkuRedisTo;
import cn.miozus.gulimall.seckill.vo.SeckillSessionWithSkus;
import cn.miozus.gulimall.seckill.vo.SeckillSkuVo;
import cn.miozus.gulimall.seckill.vo.SkuInfoVo;
import com.alibaba.fastjson.TypeReference;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 秒杀 Redis 切面
 *
 * @author miozus
 * @date 2022/03/08
 */
@Aspect
@Component
public class SeckillRedisAspect {

    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    RedissonClient redissonClient;

    private static final String SECKILL_SESSION_CACHE_PREFIX = "seckill:session:";
    private static final String SECKILL_SKUS_CACHE_PREFIX = "seckill:skus:";
    private static final String SECKILL_SKUS_STOCK_SEMAPHORE_CACHE_PREFIX = "seckill:stock:";
    private static final String SECKILL_UPLOAD_LOCK = "seckill:upload:lock";

    @Around("@annotation(idempotent)")
    public void around(ProceedingJoinPoint pjp, Idempotent idempotent) {
        RLock lock = redissonClient.getLock(SECKILL_UPLOAD_LOCK);
        lock.lock(10, TimeUnit.MINUTES);
        try {
            pjp.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Around("@annotation(putRedis)")
    public void around(ProceedingJoinPoint pjp, PutRedis putRedis) {
        List<SeckillSessionWithSkus> sessionData = (List<SeckillSessionWithSkus>) pjp.getArgs()[0];
        saveSkuIds(sessionData);
        saveRelationSkus(sessionData);
    }

    private void saveSkuIds(List<SeckillSessionWithSkus> sessionData) {
        sessionData.forEach(s -> {
            long startTime = s.getStartTime().getTime();
            long endTime = s.getEndTime().getTime();
            String key = SECKILL_SESSION_CACHE_PREFIX + startTime + "_" + endTime;
            if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                List<String> ssId = s.getRelationSkus().stream().map(
                        item -> item.getPromotionSessionId() + "_" + item.getSkuId()
                ).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, ssId);
            }
        });
    }

    /**
     * 保存sku关系
     * sku 商品详情
     * 携带随机码防作弊
     * 库存余额作为锁信号量
     * 秒杀起止时间
     * 放入远程缓存
     *
     * @param sessionData 会话数据
     */
    private void saveRelationSkus(List<SeckillSessionWithSkus> sessionData) {
        sessionData.forEach(s -> {
            long startTime = s.getStartTime().getTime();
            long endTime = s.getEndTime().getTime();
            List<SeckillSkuVo> relationSkus = s.getRelationSkus();
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SECKILL_SKUS_CACHE_PREFIX);
            relationSkus.forEach(vo -> {
                String token = UUID.randomUUID().toString(true);
                Long skuId = vo.getSkuId();
                String ssId = vo.getPromotionSessionId() + "_" + skuId;
                if (Boolean.FALSE.equals(ops.hasKey(ssId))) {
                    saveSeckillSku(startTime, endTime, ops, vo, token, skuId, ssId);
                }
            });
        });
    }

    private void saveSeckillSku(long startTime, long endTime, BoundHashOperations<String, Object, Object> ops, SeckillSkuVo vo, String token, Long skuId, String ssId) {
        SeckillSkuRedisTo to = new SeckillSkuRedisTo();
        R info = productFeignService.querySkuInfo(skuId);
        if (info.getCode() == 0) {
            SkuInfoVo skuInfoVo = info.getData(new TypeReference<SkuInfoVo>() {
            });
            to.setSkuInfo(skuInfoVo);
        }
        BeanUtils.copyProperties(vo, to);

        to.setRandomCode(token);
        to.setStartTime(startTime);
        to.setEndTime(endTime);
        String str = JSONUtil.toJsonStr(to);
        ops.put(ssId, str);

        trySetSemaphorePermits(vo, token);

    }

    /**
     * 库存限流：尝试设置信号量:
     * 因为令牌是随机生成的，靠随机值锁不住，所以用上一层场次的锁，每一次
     *
     * @param vo    签证官
     * @param token 令牌
     */
    private void trySetSemaphorePermits(SeckillSkuVo vo, String token) {
        String semaphoreKey = SECKILL_SKUS_STOCK_SEMAPHORE_CACHE_PREFIX + token;
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
        semaphore.trySetPermits(vo.getSeckillCount());
    }
}
