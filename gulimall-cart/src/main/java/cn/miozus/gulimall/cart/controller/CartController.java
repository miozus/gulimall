package cn.miozus.gulimall.cart.controller;

import cn.miozus.gulimall.cart.interceptor.CartInterceptor;
import cn.miozus.gulimall.cart.service.CartService;
import cn.miozus.gulimall.cart.to.UserInfoTo;
import cn.miozus.gulimall.cart.vo.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 购物车控制器
 *
 * @author miao
 * @date 2022/01/04
 */
@Controller
public class CartController {

    @Autowired
    CartService cartService;


    /**
     * 购物车列表页面
     * <p>
     * 最快速获取用户信息： ThreadLocal 同一个线程共享数据
     *
     * @return {@link String}
     */
    @GetMapping(value = {"/cart.html", "/cartList.html"})
    public String cartListPage() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        System.out.println("userInfoTo = " + userInfoTo);
        return "cartList";
    }

    @GetMapping("/success.html")
    public String addItem() {

        return "success";
    }

    /**
     * 加入购物车
     * <p>
     * 防刷机制：此处真实添加动作，转发至只读数据的镜像页面
     * 1 手工拼接字符串，路径名带参数转发（可选）
     * 2 重定向参数转发，地址需全称（可选👍）
     * RedirectAttributes
     * - addFlashAttribute 模拟 session，但只能取一次
     * - addAttribute 数据放在路径地址
     *
     * @param skuId 商品信息
     * @param count 数量
     * @return {@link String}
     */
    @GetMapping("/joinCart")
    public String joinCart(@RequestParam("skuId") Long skuId, @RequestParam("count") Integer count, RedirectAttributes ra) {
        cartService.joinCart(skuId, count);
        ra.addAttribute("skuId", skuId);
        return "redirect:http://cart.gulimall.com/joinCartSuccessPage.html";
    }

    /**
     * 跳转购物车加入成功页面
     *
     * @param skuId 商品信息
     * @return {@link String}
     */
    @GetMapping("/joinCartSuccessPage.html")
    public String joinCartSuccessPage(@RequestParam("skuId") Long skuId, Model model) {
        CartItem item = cartService.fetchCartItem(skuId);
        model.addAttribute("item", item);
        return "success";
    }
}
