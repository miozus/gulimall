package cn.miozus.gulimall.cart.controller;

import cn.miozus.gulimall.cart.interceptor.CartInterceptor;
import cn.miozus.gulimall.cart.to.UserInfoTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 购物车控制器
 *
 * @author miao
 * @date 2022/01/04
 */
@Controller
public class CartController {

    /**
     * 购物车列表页面
     *
     * 最快速获取用户信息 ThreadLocal 同一个线程共享数据
     *
     * @return {@link String}
     */
    @GetMapping("/cart.html")
    public String cartListPage() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        System.out.println("userInfoTo = " + userInfoTo);
        return "cartList";
    }

    @GetMapping("/success.html")
    public String addItem() {

        return "success";
    }
}
