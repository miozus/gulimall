package cn.miozus.gulimall.cart.service.impl;

import cn.miozus.common.utils.R;
import cn.miozus.gulimall.cart.feign.ProductFeignService;
import cn.miozus.gulimall.cart.interceptor.CartInterceptor;
import cn.miozus.gulimall.cart.service.CartService;
import cn.miozus.gulimall.cart.to.UserInfoTo;
import cn.miozus.gulimall.cart.vo.CartItem;
import cn.miozus.gulimall.cart.vo.SkuInfoVo;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 车服务impl
 *
 * @author miao
 * @date 2022/01/04
 */
@Service
@Slf4j
public class CartServiceImpl implements CartService {
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    ThreadPoolExecutor executor;

    private final String CART_PREFIX = "gulimall:cart:";


    /**
     * 单个物品加入购物车
     * <p>
     * 异步编程：
     * 购物车中已存在：更新数量并提交，否则按新商品计入
     * 1 远程查询商品信息
     * 2 远程查询 sku 组合信息
     * 3 封装数据 skuId:CartItem 的哈希表（这个值JSON化后）储存在 Redis
     *
     * @param skuId 商品身份证号
     * @param count 数量
     * @return {@link CartItem}
     */
    @SneakyThrows
    @Override
    public CartItem joinCart(Long skuId, Integer count) {
        BoundHashOperations<String, Object, Object> ops = boundRedisHashOps();
        String res = (String) ops.get(skuId.toString());

        if (StringUtils.isNotEmpty(res)) {
            CartItem cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount() + count);
            String s = JSON.toJSONString(cartItem);
            ops.put(skuId.toString(), s);
            return cartItem;
        }

        CartItem cartItem = new CartItem();
        CompletableFuture<Void> skuInfoFuture = CompletableFuture.runAsync(() -> {
            R info = productFeignService.info(skuId);
            SkuInfoVo skuInfo = info.getData("skuInfo", new TypeReference<SkuInfoVo>() {
            });
            BigDecimal price = new BigDecimal(skuInfo.getPrice() + "");
            cartItem.setSkuId(skuId);
            cartItem.setCount(count);
            cartItem.setCheck(true);
            cartItem.setTitle(skuInfo.getSkuTitle());
            cartItem.setImage(skuInfo.getSkuDefaultImg());
            cartItem.setPrice(price);
        }, executor);

        CompletableFuture<Void> skuAttrsFuture = CompletableFuture.runAsync(() -> {
            List<String> skuAttrs = productFeignService.querySkuAttrs(skuId);
            cartItem.setSkuAttrs(skuAttrs);
        }, executor);

        CompletableFuture.allOf(skuInfoFuture, skuAttrsFuture).get();
        String s = JSON.toJSONString(cartItem);
        ops.put(skuId.toString(), s);
        return cartItem;
    }

    /**
     * 单品加入购物车的镜像只读页面（防止刷新重复添加）
     *
     * @param skuId sku id
     * @return {@link CartItem}
     */
    @Override
    public CartItem fetchCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> ops = boundRedisHashOps();
        String res = (String) ops.get(skuId.toString());
        return JSON.parseObject(res, CartItem.class);
    }

    /**
     * 构建购物车的[操作]命令
     * cart_prefix:userId/userKey:
     * <p>
     * 从本地线程变量中获取用户信息，
     * 优先使用已登录的用户 Id ，其次临时身份
     *
     * @return {@link BoundHashOperations}<{@link String}, {@link Object}, {@link Object}>
     */
    private BoundHashOperations<String, Object, Object> boundRedisHashOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        Long userId = userInfoTo.getUserId();
        String userKey = userInfoTo.getUserKey();
        String cartKey = CART_PREFIX + (!Objects.isNull(userId) ? String.valueOf(userId) : userKey);
        return redisTemplate.boundHashOps(cartKey);
    }

}
