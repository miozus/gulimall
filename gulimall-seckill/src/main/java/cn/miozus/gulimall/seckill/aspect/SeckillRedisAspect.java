package cn.miozus.gulimall.seckill.aspect;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import cn.miozus.gulimall.common.annotation.GetRedis;
import cn.miozus.gulimall.common.annotation.Idempotent;
import cn.miozus.gulimall.common.annotation.PutRedis;
import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.seckill.feign.ProductFeignService;
import cn.miozus.gulimall.seckill.to.SeckillSkuRedisTo;
import cn.miozus.gulimall.seckill.vo.SeckillSessionWithSkus;
import cn.miozus.gulimall.seckill.vo.SeckillSkuVo;
import cn.miozus.gulimall.seckill.vo.SkuInfoVo;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.SneakyThrows;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
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
    public void lockForWriteOnce(ProceedingJoinPoint pjp, Idempotent idempotent) {
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
    public void saveEntity(ProceedingJoinPoint pjp, PutRedis putRedis) {
        List<SeckillSessionWithSkus> sessionData = (List<SeckillSessionWithSkus>) pjp.getArgs()[0];
        saveSkuIds(sessionData);
        saveRelationSkus(sessionData);
    }

    /**
     * 查询最新秒杀活动（场次/单个商品附加信息）
     *
     * @param pjp      pjp
     * @param getRedis 拉取远程缓存
     * @return {@link Object} emptyList as default
     */
    @SneakyThrows
    @Around("@annotation(getRedis)")
    public Object fetch(ProceedingJoinPoint pjp, GetRedis getRedis) {
        Object retVal = pjp.proceed();
        Object[] arg = pjp.getArgs();
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SECKILL_SKUS_CACHE_PREFIX);

        if (arg.length == 0){
            return fetchCurrentSeckillSkus(retVal, ops);
        }
        return fetchSeckillSku(retVal, arg[0], ops);
    }

    private Object fetchCurrentSeckillSkus(Object retVal, BoundHashOperations<String, String, String> ops) {
        Set<String> keys = redisTemplate.keys(SECKILL_SESSION_CACHE_PREFIX + "*");
        if (CollectionUtils.isEmpty(keys)) {
            return retVal;
        }
        for (String key : keys) {
            if (!isExpiryDateString(key)) {
                continue;
            }
            List<String> range = redisTemplate.opsForList().range(key, -100, 100);
            if (CollectionUtils.isEmpty(range)) {
                continue;
            }
            List<String> objects = ops.multiGet(range);
            if (CollectionUtils.isEmpty(objects)) {
                continue;
            }
            // 当前时间段最多1个场次（每个场次有许多商品）
            return objects.stream().map(objStr -> JSONUtil.toBean(objStr, SeckillSkuRedisTo.class))
                    .collect(Collectors.toList());
        }
        return retVal;
    }

    private Object fetchSeckillSku(Object retVal, Object arg, BoundHashOperations<String, String, String> ops) {
        Set<String> keys = ops.keys();
        if (CollectionUtils.isEmpty(keys)) {
            return retVal;
        }
        String regex = "\\d_" + arg;
        for (String key : keys) {
            if (Pattern.matches(regex, key)) {
                String object = ops.get(key);
                SeckillSkuRedisTo redisTo = JSONUtil.toBean(object, SeckillSkuRedisTo.class);
                if (!redisTo.isExpiryDate()) {
                    redisTo.setRandomCode("");
                }
                return redisTo;
            }
        }
        return retVal;
    }

    private boolean isExpiryDateString(String key) {
        String[] interval = key.replace(SECKILL_SESSION_CACHE_PREFIX, "").split("_");
        long now = DateUtil.current();
        long start = Long.parseLong(interval[0]);
        long end = Long.parseLong(interval[1]);
        return start <= now && now <= end;
    }


    private void saveSkuIds(List<SeckillSessionWithSkus> sessionData) {
        sessionData.forEach(s -> {
            long startTime = s.getStartTime().getTime();
            long endTime = s.getEndTime().getTime();
            String key = SECKILL_SESSION_CACHE_PREFIX + startTime + "_" + endTime;
            if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                List<String> killId = s.getRelationSkus().stream().map(
                        item -> item.getPromotionSessionId() + "_" + item.getSkuId()
                ).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, killId);
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
                String killId = vo.getPromotionSessionId() + "_" + skuId;
                if (Boolean.FALSE.equals(ops.hasKey(killId))) {
                    saveSeckillSku(startTime, endTime, ops, vo, token, skuId, killId);
                }
            });
        });
    }

    private void saveSeckillSku(long startTime, long endTime, BoundHashOperations<String, Object, Object> ops, SeckillSkuVo vo, String token, Long skuId, String killId) {
        SeckillSkuRedisTo to = new SeckillSkuRedisTo();
        R r = productFeignService.querySkuInfo(skuId);
        if (r.getCode() == 0) {
            SkuInfoVo skuInfoVo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
            });
            to.setSkuInfo(skuInfoVo);
        }
        BeanUtils.copyProperties(vo, to);

        to.setRandomCode(token);
        to.setStartTime(startTime);
        to.setEndTime(endTime);

        String str = JSONUtil.toJsonStr(to);
        ops.put(killId, str);

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
