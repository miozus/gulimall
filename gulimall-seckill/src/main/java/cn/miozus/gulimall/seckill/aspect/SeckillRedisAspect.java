package cn.miozus.gulimall.seckill.aspect;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import cn.miozus.gulimall.common.annotation.GetRedis;
import cn.miozus.gulimall.common.annotation.Idempotent;
import cn.miozus.gulimall.common.annotation.PutRedis;
import cn.miozus.gulimall.common.exception.GuliMallBindException;
import cn.miozus.gulimall.common.to.mq.SeckillOrderTo;
import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.common.vo.MemberRespVo;
import cn.miozus.gulimall.seckill.feign.ProductFeignService;
import cn.miozus.gulimall.seckill.interceptor.LoginUserInterceptor;
import cn.miozus.gulimall.seckill.to.SeckillSkuRedisTo;
import cn.miozus.gulimall.seckill.vo.SeckillSessionWithSkus;
import cn.miozus.gulimall.seckill.vo.SeckillSkuVo;
import cn.miozus.gulimall.seckill.vo.SkuInfoVo;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
@Order(1)
@Slf4j
public class SeckillRedisAspect {

    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    LoginUserInterceptor loginUserInterceptor;

    private static final String SECKILL_SESSION_CACHE_PREFIX = "seckill:session:";
    private static final String SECKILL_SKUS_CACHE_PREFIX = "seckill:skus:";
    private static final String SECKILL_SKUS_STOCK_SEMAPHORE_CACHE_PREFIX = "seckill:stock:";
    private static final String SECKILL_UPLOAD_LOCK = "seckill:upload:lock";


    @SneakyThrows
    @Around("@annotation(idempotent)")
    public Object idempotentRedis(ProceedingJoinPoint pjp, Idempotent idempotent) {
        String comment = idempotent.value();
        switch (comment) {
            case "秒杀商品上架加锁":
                return lockForUploadOnce(pjp);
            case "校验秒杀请求全字段":
                return verifySeckillParams(pjp);
            default:
                return pjp.proceed();
        }
    }

    private Object lockForUploadOnce(ProceedingJoinPoint pjp) {
        RLock lock = redissonClient.getLock(SECKILL_UPLOAD_LOCK);
        lock.lock(10, TimeUnit.MINUTES);
        Object retVal;
        try {
            retVal = pjp.proceed();
        } catch (Throwable e) {
            throw new GuliMallBindException(e.getMessage());
        } finally {
            lock.unlock();
        }
        return retVal;
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
        Object[] args = pjp.getArgs();
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SECKILL_SKUS_CACHE_PREFIX);

        if (args.length == 0) {
            return fetchCurrentSeckillSkus(retVal, ops);
        }
        return fetchSeckillSku(retVal, ops, args[0]);
    }

    /**
     * 秒杀新流程
     * 校验秒杀请求参数和幂等性加锁
     *
     * @param pjp pjp
     * @return {@link Object}
     * @throws Throwable throwable
     */
    private Object verifySeckillParams(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        String killId = (String) args[0];
        String key = (String) args[1];
        Integer num = (Integer) args[2];
        // 全参数合法性校验
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SECKILL_SKUS_CACHE_PREFIX);
        String redis = ops.get(killId);
        if (StringUtils.isEmpty(redis)) {
            return null;
        }
        SeckillSkuRedisTo to = JSONUtil.toBean(redis, SeckillSkuRedisTo.class);
        if (to.isNotExpiryDate()) {
            return null;
        }
        Long skuId = to.getSkuId();
        Long promotionSessionId = to.getPromotionSessionId();
        String ssId = promotionSessionId + "_" + skuId;
        if (ObjectUtil.notEqual(key, to.getRandomCode()) || ObjectUtil.notEqual(killId, ssId)) {
            return null;
        }
        if ((num > to.getSeckillLimit())) {
            return null;
        }
        // 幂等性占位，获取信号量，发送消息队列
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUserThreadLocal.get();
        Long memberId = memberRespVo.getId();
        String occupyKey = memberId + "_" + ssId;
        Boolean occupied = redisTemplate.opsForValue().setIfAbsent(occupyKey, num.toString(), to.getTtl(), TimeUnit.MILLISECONDS);
        if (Boolean.FALSE.equals(occupied)) {
            return null;
        }
        RSemaphore semaphore = redissonClient.getSemaphore(SECKILL_SKUS_STOCK_SEMAPHORE_CACHE_PREFIX + key);
        boolean acquire = semaphore.tryAcquire(num, 100, TimeUnit.MILLISECONDS);
        if (acquire) {
            String orderSn = IdWorker.getTimeId();
            SeckillOrderTo orderTo = new SeckillOrderTo();
            orderTo.setNum(num);
            orderTo.setSkuId(skuId);
            orderTo.setMemberId(memberId);
            orderTo.setSeckillPrice(to.getSeckillPrice());
            orderTo.setPromotionSessionId(promotionSessionId);
            orderTo.setOrderSn(orderSn);
            // 强制修改参数，通过异常返回正常流程，而通过AOP消息队列处理收尾动作
            try {
                return pjp.proceed(new Object[]{orderTo, null, null});
            } catch (Throwable e) {
                return orderSn;
            }
        }
        return null;
    }

    @SuppressWarnings({"squid:S2259", "squid:S4449", "ConstantConditions"})
    private Object fetchCurrentSeckillSkus(Object retVal, BoundHashOperations<String, String, String> ops) {
        try {
            Set<String> keys = redisTemplate.keys(SECKILL_SESSION_CACHE_PREFIX + "*");
            for (String key : keys) {
                if (isExpiryDateString(key)) {
                    List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                    List<String> objects = ops.multiGet(range);
                    // 当前时间段最多1个场次（每个场次有许多商品）
                    return objects.stream().map(objStr -> JSONUtil.toBean(objStr, SeckillSkuRedisTo.class))
                            .collect(Collectors.toList());
                }
            }
        } catch (NullPointerException e) {
            log.debug("未扫描到应该上架的秒杀活动商品，小概率可能网络故障。" + e.getMessage());
            return retVal;
        }
        return retVal;
    }

    private Object fetchSeckillSku(Object retVal, BoundHashOperations<String, String, String> ops, Object arg) {
        Set<String> keys = ops.keys();
        if (CollectionUtils.isEmpty(keys)) {
            return retVal;
        }
        String regex = "\\d_" + arg;
        for (String key : keys) {
            if (Pattern.matches(regex, key)) {
                String object = ops.get(key);
                SeckillSkuRedisTo redisTo = JSONUtil.toBean(object, SeckillSkuRedisTo.class);
                if (redisTo.isNotExpiryDate()) {
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
        if (r.isOk()) {
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
     * 库存限流：尝试设置信号量
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
