package cn.miozus.gulimall.cart.service.impl;

import cn.miozus.common.utils.R;
import cn.miozus.gulimall.cart.feign.ProductFeignService;
import cn.miozus.gulimall.cart.interceptor.CartInterceptor;
import cn.miozus.gulimall.cart.service.CartService;
import cn.miozus.gulimall.cart.to.UserInfoTo;
import cn.miozus.gulimall.cart.vo.Cart;
import cn.miozus.gulimall.cart.vo.CartItem;
import cn.miozus.gulimall.cart.vo.SkuInfoVo;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.common.utils.CollectionUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     * @param skuId 库存商品身份证号
     * @param count 数量
     * @return {@link CartItem}
     */
    @SneakyThrows
    @Override
    public CartItem addToCart(Long skuId, Integer count) {
        BoundHashOperations<String, Object, Object> ops = boundUserIdFirstRedisHashOps();
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
            cartItem.setIsChecked(true);
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
     * @param skuId 库存商品身份证号
     * @return {@link CartItem}
     */
    @Override
    public CartItem fetchCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> ops = boundUserIdFirstRedisHashOps();
        String res = (String) ops.get(skuId.toString());
        return JSON.parseObject(res, CartItem.class);
    }

    /**
     * 获取总车项目
     * <p>
     * （但可能两人共用电脑，将别人的购物清单并入了，应该砍掉离线购物车的需求）
     * <p>
     * 未登录：只算离线购物车，直接短路返回
     * 才登录：离线购物车（非空，转移后清空） + 在线购物车（遍历调用 addToCart 添加，但调用两次本地远程服务 / 合并两个列表，逐个提交 👍）
     *
     * @return {@link Cart}
     */
    @Override
    public Cart fetchTotalCartItems() {
        Cart cart = new Cart();
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        Long userId = userInfoTo.getUserId();

        String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
        List<CartItem> tempCartItems = collectRedisCartItems(tempCartKey);

        if (Objects.isNull(userId)) {
            cart.setItems(tempCartItems);
            return cart;
        }

        String oauthCartKey = CART_PREFIX + userId;
        List<CartItem> oauthCartItems = collectRedisCartItems(oauthCartKey);

        if (CollectionUtils.isNotEmpty(tempCartItems)) {
            oauthCartItems =  addAllRedisCartItemsByKey(tempCartItems, oauthCartItems, oauthCartKey);
        }
        cart.setItems(oauthCartItems);
        //deleteRedisCartItems(tempCartKey);
        return cart;
    }

    /**
     * 合并两个购物车单品：离线+在线（同类合并和更新数量）
     * 离线购物车 =
     * 重复：更新数量
     * 未重复：加入在线购物车
     * <p>
     * 离线购物车转成集合，去重（更新并删除重复）后，更新Redis
     *
     * @param tempCartItems  离线购物车
     * @param oauthCartKey   登录用户购物车的键
     * @param oauthCartItems 登录用户购物车的单品
     * @return {@link List}<{@link CartItem}> 合并后用户在线购物车商品
     */
    private List<CartItem> addAllRedisCartItemsByKey(List<CartItem> tempCartItems, List<CartItem> oauthCartItems, String oauthCartKey) {
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(oauthCartKey);
        Map<Long, CartItem> tempCartItemsMap = tempCartItems.stream().collect(Collectors.toMap(CartItem::getSkuId, Function.identity()));
        List<CartItem> newOauthCartItems = oauthCartItems.stream().map(v -> {
            Long skuId = v.getSkuId();
            if (tempCartItemsMap.containsKey(skuId)) {
                int updateCount = tempCartItemsMap.get(skuId).getCount() + v.getCount();
                v.setCount(updateCount);
                String s = JSON.toJSONString(v);
                ops.put(skuId.toString(), s);
                tempCartItemsMap.remove(skuId);
            }
            return v;
        }).collect(Collectors.toList());
        List<CartItem> newTempCartItems = tempCartItemsMap.entrySet().stream().map(e -> {
            Long skuId = e.getKey();
            CartItem cartItem = e.getValue();
            String s = JSON.toJSONString(cartItem);
            ops.put(skuId.toString(), s);
            return cartItem;
        }).collect(Collectors.toList());
        newOauthCartItems.addAll(newTempCartItems);
        return newOauthCartItems;
    }

    private void deleteRedisCartItems(String tempCartKey) {
        redisTemplate.delete(tempCartKey);
    }

    /**
     * 收集,购物车条目
     *
     * redis.value ⇒ java.List
     *
     * @param cartKey 购物车键
     * @return {@link List}<{@link CartItem}>
     */
    private List<CartItem> collectRedisCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(cartKey);
        List<Object> values = ops.values();
        if (Objects.nonNull(values)) {
            return values.stream().map((value) ->
                    JSON.parseObject((String) value, CartItem.class)).collect(Collectors.toList());
        }
        return Collections.emptyList();
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
