package cn.miozus.gulimall.cart.service;

import cn.miozus.gulimall.cart.vo.Cart;
import cn.miozus.gulimall.cart.vo.CartItem;

import java.util.List;

/**
 * 购物车服务
 *
 * @author miao
 * @date 2022/01/04
 */
public interface CartService {
    /**
     * 合并购物车：因为只有 Bean 可以被 AOP 代理
     *
     * @param tempCartItems  离线购物车
     * @param oauthCartItems 登录购物车
     * @return {@link List}<{@link CartItem}>
     */
    List<CartItem> addAllRedisCartItemsByKey(List<CartItem> tempCartItems, List<CartItem> oauthCartItems);

    /**
     * 加入购物车
     *
     * @param skuId sku id
     * @param count 数
     * @return {@link CartItem}
     */
    CartItem addToCart(Long skuId, Integer count);

    /**
     * 单品加入购物车的镜像只读页面
     *
     * @param skuId sku id
     * @return {@link CartItem}
     */
    CartItem fetchCartItem(Long skuId);

    /**
     * 获取总车项目
     *
     * @return {@link Cart}
     */
    Cart fetchTotalCartItems();

    /**
     * 更新:是否勾选
     *  @param skuId     sku id
     * @param isChecked 检查
     * @return
     */
    boolean updateRedisItemCheckStatus(Long skuId, Integer isChecked);

    /**
     * 更新：数量
     *  @param skuId sku id
     * @param count 数
     * @return
     */
    Integer updateRedisItemCount(Long skuId, Integer count);

    /**
     * 删除：购物车单品
     *
     * @param skuId sku id
     */
    void deleteRedisItem(Long skuId);

    /**
     * 获取当前购物车条目（结算页）
     *
     * @return {@link List}<{@link CartItem}>
     */
    List<CartItem> fetchOrderCartItems();

    /**
     * 查询购物车单品并转成列表
     * <p>
     * redis.value ⇒ java.List
     *
     * @param cartKey 购物车键
     * @return {@link List}<{@link CartItem}>
     */
    List<CartItem> collectRedisCartItems( String cartKey) ;

}
