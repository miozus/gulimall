package cn.miozus.gulimall.cart.service.impl;

import cn.miozus.common.annotation.DeleteRedis;
import cn.miozus.common.annotation.GetRedis;
import cn.miozus.common.annotation.PutRedis;
import cn.miozus.common.utils.R;
import cn.miozus.gulimall.cart.feign.ProductFeignService;
import cn.miozus.gulimall.cart.interceptor.CartInterceptor;
import cn.miozus.gulimall.cart.service.CartService;
import cn.miozus.gulimall.cart.to.UserInfoTo;
import cn.miozus.gulimall.cart.vo.Cart;
import cn.miozus.gulimall.cart.vo.CartItem;
import cn.miozus.gulimall.cart.vo.SkuInfoVo;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.common.utils.CollectionUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车服务实现
 *
 * @author miao
 * @date 2022/01/04
 */
@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    ThreadPoolExecutor executor;
    @Autowired
    private CartService cartService;

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
    @PutRedis("更新数量或新增单品")
    public CartItem addToCart(Long skuId, Integer count) {
        CartItem cartItem = cartService.fetchCartItem(skuId);
        if (Objects.nonNull(cartItem)) {
            int sum = cartItem.getCount() + count;
            cartItem.setCount(sum);
            return cartItem;
        }
        return buildCartItemAsync(skuId, count);
    }


    private CartItem buildCartItemAsync(Long skuId, Integer count) throws InterruptedException, ExecutionException {
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
        return cartItem;
    }

    /**
     * 单品加入购物车的镜像只读页面（防止刷新重复添加）
     *
     * @param skuId 库存商品身份证号
     * @return {@link CartItem}
     */
    @Override
    @GetRedis("获取单品镜像")
    public CartItem fetchCartItem(@GetRedis("key") Long skuId) {
        return null;
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

        if (Objects.isNull(userId)) {
            List<CartItem> tempCartItems = cartService.collectRedisCartItems(tempCartKey);
            cart.setItems(tempCartItems);
            return cart;
        }

        String oauthCartKey = CART_PREFIX + userId;
        return addAllCartItems(cart, oauthCartKey, tempCartKey);
    }

    private Cart addAllCartItems(Cart cart, String oauthCartKey, String tempCartKey) {

        List<CartItem> oauthCartItems = cartService.collectRedisCartItems(oauthCartKey);
        List<CartItem> tempCartItems = cartService.collectRedisCartItems(tempCartKey);

        if (CollectionUtils.isNotEmpty(tempCartItems)) {
            oauthCartItems = cartService.addAllRedisCartItemsByKey(tempCartItems, oauthCartItems);
        }
        cart.setItems(oauthCartItems);
        return cart;
    }

    /**
     * 更新勾选状态
     * 有 2 次 查询 Redis
     * <p>
     * AOP：前后重复的动作，真应该放入 切面 去完成
     *
     * @param skuId     sku id
     * @param isChecked 检查
     * @return {@link CartItem}
     */
    @Override
    @PutRedis("勾选状态")
    public boolean updateRedisItemCheckStatus(Long skuId, Integer isChecked) {
        return isChecked == 1;
    }

    @Override
    @PutRedis("商品数量")
    public Integer updateRedisItemCount(Long skuId, Integer count) {
        return count;
    }


    @Override
    @DeleteRedis("删除单品")
    public void deleteRedisItem(Long skuId) {
        // 注解抽取参数代为执行
    }

    /**
     * 获取当前购物车商品
     * <p>
     * 未登录：（通过URL访问），不予查询
     * 登录：商品服务查询最新价格
     * 筛选：只返回缓存中已勾选的商品
     *
     * @return {@link List}<{@link CartItem}>
     */
    @Override
    public List<CartItem> fetchOrderCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        Long userId = userInfoTo.getUserId();
        if (Objects.isNull(userId)) {
            return Collections.emptyList();
        }
        return cartService.fetchCheckedOrderCartItems(userId);
    }

    @Override
    public List<CartItem> fetchCheckedOrderCartItems(Long userId) {
        String oauthCartKey = CART_PREFIX + userId;
        List<CartItem> cartItems = cartService.collectRedisCartItems(oauthCartKey);
        return cartItems.stream()
                .map(item -> {
                    Long skuId = item.getSkuId();
                    BigDecimal price = productFeignService.querySkuPrice(skuId);
                    item.setPrice(price);
                    return item;
                }).filter(CartItem::getIsChecked).collect(Collectors.toList());
    }

    @Override
    @DeleteRedis("删除已付款购物车商品")
    //@CacheEvict(value = "orderSubmitted", key = "'uid'+#uid")
    public Boolean  deleteOrderCartItems() {
        return false;
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
     * @param oauthCartItems 登录用户购物车的单品
     * @return {@link List}<{@link CartItem}>
     */
    @Override
    @PutRedis("合并购物车")
    @DeleteRedis("合并后删除离线购物车缓存")
    public List<CartItem> addAllRedisCartItemsByKey(List<CartItem> tempCartItems, List<CartItem> oauthCartItems) {
        Map<Long, CartItem> tempCartItemsMap = tempCartItems.stream().collect(
                Collectors.toMap(CartItem::getSkuId, Function.identity()));
        List<CartItem> newOauthCartItems = oauthCartItems.stream().map(v -> {
            Long skuId = v.getSkuId();
            if (tempCartItemsMap.containsKey(skuId)) {
                int updateCount = tempCartItemsMap.get(skuId).getCount() + v.getCount();
                v.setCount(updateCount);
                tempCartItemsMap.remove(skuId);
            }
            return v;
        }).collect(Collectors.toList());
        List<CartItem> newTempCartItems = tempCartItemsMap.entrySet().stream().map(Map.Entry::getValue)
                .collect(Collectors.toList());
        newOauthCartItems.addAll(newTempCartItems);
        return newOauthCartItems;
    }

    /**
     * 查询购物车单品并转成列表
     * <p>
     * redis.value ⇒ java.List
     *
     * @param cartKey 购物车键
     * @return {@link List}<{@link CartItem}>
     */
    @GetRedis("获取整车商品")
    public List<CartItem> collectRedisCartItems(@GetRedis("key") String cartKey) {
        return Collections.emptyList();
    }

}
