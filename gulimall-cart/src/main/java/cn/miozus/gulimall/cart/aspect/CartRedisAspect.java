package cn.miozus.gulimall.cart.aspect;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.miozus.gulimall.common.annotation.DeleteRedis;
import cn.miozus.gulimall.common.annotation.GetRedis;
import cn.miozus.gulimall.common.annotation.PutRedis;
import cn.miozus.gulimall.common.enume.BizCodeEnum;
import cn.miozus.gulimall.common.exception.GuliMallBindException;
import cn.miozus.gulimall.cart.interceptor.CartInterceptor;
import cn.miozus.gulimall.cart.to.UserInfoTo;
import cn.miozus.gulimall.cart.vo.CartItem;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Redis 操作切面
 *
 * @author miao
 * @date 2022/02/08
 */
@Aspect
@Component
@Slf4j
public class CartRedisAspect {

    @Autowired
    StringRedisTemplate redisTemplate;

    private final String CART_PREFIX = "gulimall:cart:";

    /**
     * 拉取最新缓存
     *
     * @param key      键
     * @param pjp      托管方法
     * @param getRedis 注解:方法 + 参数
     * @return {@link Object}
     * @throws Throwable throwable
     */
    @Around(value = "@annotation(getRedis) && args(key)")
    public Object queryCartItemRedis(ProceedingJoinPoint pjp, GetRedis getRedis, Object key) throws Throwable {
        String clazz = key.getClass().getSimpleName();
        switch (clazz) {
            case "Long": {
                return fetchOneBySkuId(key);
            }
            case "String": {
                return fetchListByCartKey(key);
            }
            default:
        }
        return pjp.proceed();
    }

    private CartItem fetchOneBySkuId(Object key) {
        BoundHashOperations<String, Object, Object> ops = boundUserIdFirstRedisHashOps();
        String skuId = (String) ops.get(key.toString());
        if (StringUtils.isNotEmpty(skuId)) {
            return JSON.parseObject(skuId, CartItem.class);
        }
        return null;
    }

    private List<CartItem> fetchListByCartKey(Object key) {
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(key.toString());
        List<Object> res = ops.values();
        if (CollectionUtils.isNotEmpty(res)) {
            return res.stream().map(value ->
                    JSON.parseObject((String) value, CartItem.class)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 同步更新缓存
     *
     * @param point    切入点：首位参数：List<CartItem> 合并购物车，CartItem 更新该单品， skuId 根据 skuId 查询并拷贝更新
     * @param retVal   返回值
     * @param putRedis 新增或更新的注解
     */
    @AfterReturning(value = "@annotation(putRedis)", returning = "retVal")
    public void updateCartItemAttributeRedis(JoinPoint point, Object retVal, PutRedis putRedis) {
        BoundHashOperations<String, Object, Object> ops = boundUserIdFirstRedisHashOps();
        String clazz = retVal.getClass().getSimpleName();
        CartItem cartItem = new CartItem();
        switch (clazz) {
            case "CartItem": {
                cartItem = (CartItem) retVal;
                break;
            }
            case "Boolean": {
                cartItem.setIsChecked((Boolean) retVal);
                break;
            }
            case "Integer": {
                cartItem.setCount((Integer) retVal);
                break;
            }
            case "ArrayList": {
                List<CartItem> cartItems = (List<CartItem>) retVal;
                for (CartItem item : cartItems) {
                    update(item, ops, point);
                }
                return;
            }
            default:
        }
        update(cartItem, ops, point);
    }

    /**
     * 删除 Redis 缓存同步
     *
     * @param deleteRedis 删除缓存注解
     * @param pjp         进程切点
     * @return {@link Object}
     */
    @Around(value = "@annotation(deleteRedis)")
    public Object deleteCartItemByKeyRedis(ProceedingJoinPoint pjp, DeleteRedis deleteRedis) {
        Object retVal = null;
        try {
            retVal = pjp.proceed();
            BoundHashOperations<String, Object, Object> ops = boundUserIdFirstRedisHashOps();
            if (Objects.isNull(retVal)) {
                String skuId = pjp.getArgs()[0].toString();
                ops.delete(skuId);
            } else {
                String clazz = retVal.getClass().getSimpleName();
                if ("ArrayList".equals(clazz)) {
                    String tempCartKey = getTempCartKey();
                    redisTemplate.delete(tempCartKey);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new GuliMallBindException(BizCodeEnum.REDIS_CONDITION_DEPEND_PREVIEW_EXCEPTION);
        }
        return retVal;
    }

    private String getTempCartKey() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        return CART_PREFIX + userInfoTo.getUserKey();
    }

    private void update(CartItem cartItem, BoundHashOperations<String, Object, Object> ops, JoinPoint point) {
        Long skuId = cartItem.getSkuId();
        if (Objects.nonNull(skuId)) {
            String update = JSON.toJSONString(cartItem);
            ops.put(skuId.toString(), update);
            return;
        }
        String skuIdParam = point.getArgs()[0].toString();
        String remoteStr = (String) ops.get(skuIdParam);
        if (StringUtils.isEmpty(remoteStr)) {
            return;
        }
        CartItem remoteObj = JSON.parseObject(remoteStr, CartItem.class);
        BeanUtil.copyProperties(cartItem, remoteObj, CopyOptions.create().ignoreNullValue().setIgnoreError(true));
        String update = JSON.toJSONString(remoteObj);
        ops.put(skuIdParam, update);
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
    private BoundHashOperations<String, Object, Object> boundUserIdFirstRedisHashOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        Long userId = userInfoTo.getUserId();
        String userKey = userInfoTo.getUserKey();
        String cartKey = CART_PREFIX + (Objects.nonNull(userId) ? String.valueOf(userId) : userKey);
        return redisTemplate.boundHashOps(cartKey);
    }


}
