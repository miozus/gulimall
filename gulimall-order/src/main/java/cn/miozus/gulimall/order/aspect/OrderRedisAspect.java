package cn.miozus.gulimall.order.aspect;


import cn.miozus.common.annotation.DeleteRedis;
import cn.miozus.common.vo.MemberRespVo;
import cn.miozus.gulimall.order.entity.OrderItemEntity;
import cn.miozus.gulimall.order.interceptor.LoginUserInterceptor;
import cn.miozus.gulimall.order.vo.OrderSubmitRespVo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

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

    /**
     * 删除缓存
     */
    @Around(value = "@annotation(deleteRedis)")
    public Object deleteRedisCacheSync(ProceedingJoinPoint pjp,  DeleteRedis deleteRedis) throws Throwable {
        BoundHashOperations<String, Object, Object> ops = boundUserIdRedisHashOps();
        Object retVal = pjp.proceed();
        String clazz = retVal.getClass().getSimpleName();
        if ("OrderSubmitRespVo".equals(clazz)) {
            OrderSubmitRespVo response = (OrderSubmitRespVo) retVal;
            List<OrderItemEntity> orderItems = response.getOrder().getOrderItems();
            if (Objects.nonNull(orderItems)) {
                orderItems.forEach(orderItem -> ops.delete(orderItem.getSkuId().toString()));
            }
        }
        return retVal;
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


}
