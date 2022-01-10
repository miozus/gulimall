package cn.miozus.gulimall.cart.service;

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
    CartItem joinCart(Long skuId, Integer count);

    /**
     * 单品加入购物车的镜像只读页面
     *
     * @param skuId sku id
     * @return {@link CartItem}
     */
    CartItem fetchCartItem(Long skuId);
}
