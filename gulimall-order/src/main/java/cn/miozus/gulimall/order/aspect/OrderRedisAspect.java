package cn.miozus.gulimall.order.aspect;


import cn.miozus.gulimall.common.annotation.DeleteRedis;
import cn.miozus.gulimall.common.annotation.Idempotent;
import cn.miozus.gulimall.common.annotation.PutRedis;
import cn.miozus.gulimall.common.constant.OrderConstant;
import cn.miozus.gulimall.common.enume.BizCodeEnum;
import cn.miozus.gulimall.common.exception.GuliMallBindException;
import cn.miozus.gulimall.common.vo.MemberRespVo;
import cn.miozus.gulimall.order.entity.OrderEntity;
import cn.miozus.gulimall.order.entity.OrderItemEntity;
import cn.miozus.gulimall.order.interceptor.LoginUserInterceptor;
import cn.miozus.gulimall.order.vo.OrderConfirmVo;
import cn.miozus.gulimall.order.vo.OrderSubmitVo;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 购物车商品缓存切面
 *
 * @author miozus
 * @date 2022/02/16
 */
@Aspect
@Component
@Order(1)
public class OrderRedisAspect {

    @Autowired
    StringRedisTemplate redisTemplate;

    private final String CART_PREFIX = "gulimall:cart:";


    @Around(value = "@annotation(idempotent)")
    public Object checkIdempotentRedisCache(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        OrderSubmitVo args = (OrderSubmitVo) pjp.getArgs()[0];
        Long execute = deleteKeyIfExistTokenRedis(args);
        if (execute == 0L) {
            throw new GuliMallBindException(BizCodeEnum.REDIS_TOKEN_INVALID_EXCEPTION);
        }
        return pjp.proceed();
    }


    /**
     * 删除缓存
     */
    @Around(value = "@annotation(deleteRedis)")
    public Object deleteCacheRedis(ProceedingJoinPoint pjp, DeleteRedis deleteRedis) throws Throwable {
        Object retVal = pjp.proceed();
        BoundHashOperations<String, Object, Object> ops = boundUserIdRedisHashOps();
        String clazz = retVal.getClass().getSimpleName();
        if ("OrderEntity".equals(clazz)) {
            OrderEntity response = (OrderEntity) retVal;
            List<OrderItemEntity> orderItems = response.getOrderItems();
            if (Objects.nonNull(orderItems)) {
                orderItems.forEach(orderItem -> ops.delete(orderItem.getSkuId().toString()));
            }
        }
        return retVal;
    }

    @AfterReturning(value="@annotation(putRedis)", returning = "retVal")
    public Object submitTokenRedis(JoinPoint joinPoint, Object retVal, PutRedis putRedis) throws Throwable {
        String token = generateTokenRedis();
        OrderConfirmVo confirmVo = (OrderConfirmVo) retVal;
        confirmVo.setOrderToken(token);
        return confirmVo;
    }
    
    /**
     * 绑定购物车的 Redis 操作命令
     * cart_prefix:userId/userKey:
     * <p>
     * 从本地线程变量中获取用户信息，
     * 优先使用已登录的用户 Id ，其次临时身份（该缓存持续时间一个月）
     *
     * @return {@link BoundHashOperations}<{@link String}, {@link Object}, {@link Object}>
     */
    private BoundHashOperations<String, Object, Object> boundUserIdRedisHashOps() {
        MemberRespVo memberRespVo = LoginUserInterceptor.threadLocal.get();
        String userId = memberRespVo.getId().toString();
        String cartKey = CART_PREFIX + userId;
        return redisTemplate.boundHashOps(cartKey);
    }


    /**
     * 校验并删除缓存令牌
     * ⚛️ 必须使用脚本锁定原子操作
     *
     * @param orderSubmitVo 订单提交签证官
     * @return {@link Long}
     */
    private Long deleteKeyIfExistTokenRedis(OrderSubmitVo orderSubmitVo) {
        MemberRespVo loginUser = LoginUserInterceptor.threadLocal.get();
        Long uid = loginUser.getId();
        String tokenKey = OrderConstant.ORDER_USER_TOKEN_PREFIX + uid;
        String token = orderSubmitVo.getOrderToken();
//        return RedisUtil.deleteKeyIfExist(tokenKey, token);
        String deleteKeyIfExistsLuaScript = "if redis.call('get',KEYS[1]) == ARGV[1] then " +
                "return redis.call('del',KEYS[1]); " +
                "else " +
                "return 0; " +
                "end; ";
        DefaultRedisScript<Long> action = new DefaultRedisScript<>(deleteKeyIfExistsLuaScript, Long.class);
        return redisTemplate.execute(action, Collections.singletonList(tokenKey), token);
    }


    private String generateTokenRedis() {
        MemberRespVo loginUser = LoginUserInterceptor.threadLocal.get();
        Long uid = loginUser.getId();
        String tokenKey = OrderConstant.ORDER_USER_TOKEN_PREFIX + uid;
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(tokenKey, token, 30, TimeUnit.MINUTES);
        return token;
    }
}
