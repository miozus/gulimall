package cn.miozus.gulimall.cart.service;

import cn.miozus.gulimall.cart.vo.Cart;
import cn.miozus.gulimall.cart.vo.CartItem;

/**
 * 购物车服务
 *
 * @author miao
 * @date 2022/01/04
 */
public interface CartService {
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
     *
     * @param skuId     sku id
     * @param isChecked 检查
     */
    void updateRedisItemCheckStatus(Long skuId, Integer isChecked);

    /**
     * 更新：数量
     *
     * @param skuId sku id
     * @param count 数
     */
    void updateRedisItemCount(Long skuId, Integer count);

    /**
     * 删除：购物车单品
     *
     * @param skuId sku id
     */
    void deleteRedisItem(Long skuId);
}
