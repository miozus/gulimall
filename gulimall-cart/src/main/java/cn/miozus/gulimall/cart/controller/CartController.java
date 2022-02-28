package cn.miozus.gulimall.cart.controller;

import cn.miozus.gulimall.cart.service.CartService;
import cn.miozus.gulimall.cart.vo.Cart;
import cn.miozus.gulimall.cart.vo.CartItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 购物车控制器
 *
 * @author miao
 * @date 2022/01/04
 */
@Controller
@Slf4j
public class CartController {

    @Autowired
    CartService cartService;

    @GetMapping("/cartItems")
    @ResponseBody
    public List<CartItem> fetchOrderCartItems() throws Throwable {
        return cartService.fetchCheckedOrderCartItems();
    }

    /**
     * 改：是否勾选
     * <p>
     * 前端 ⇒ 后端：更新 Redis 缓存 ⇒ 后端：访问购物车列表
     *
     * @param skuId     sku id
     * @param isChecked 检查
     * @return {@link String}
     */
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("isChecked") Integer isChecked) {
        cartService.updateRedisItemCheckStatus(skuId, isChecked);
        return "redirect:http://cart.gulimall.com/cartList.html";
    }

    /**
     * 改：单品数量
     *
     * @param skuId sku id
     * @param count 数
     * @return {@link String}
     */
    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("count") Integer count) {
        cartService.updateRedisItemCount(skuId, count);
        return "redirect:http://cart.gulimall.com/cartList.html";
    }

    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteRedisItem(skuId);
        return "redirect:http://cart.gulimall.com/cartList.html";
    }

    /**
     * 购物车列表页面
     * <p>
     * 最快速获取用户信息： ThreadLocal 同一个线程共享数据
     * 从 Redis 查询所有单品，如果从临时身份切换到登录，则合并购物车
     *
     * @return {@link String}
     */
    @GetMapping("/cartList.html")
    public String cartListPage(Model model) {
        Cart cart = cartService.fetchTotalCartItems();
        log.debug("cart {} ", cart);
        model.addAttribute("cart", cart);
        return "cartList";
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
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("count") Integer count,  @RequestParam("itemUrl") String itemUrl, RedirectAttributes ra) {
        try {
            cartService.addToCart(skuId, count);
            ra.addAttribute("skuId", skuId);
            return "redirect:http://cart.gulimall.com/addToCartSuccessPage.html";
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            ra.addFlashAttribute("msg", "加入购物车失败，请稍后再试");
            return "redirect:" + itemUrl;
        }
    }

    /**
     * 跳转购物车加入成功页面
     *
     * @param skuId 商品信息
     * @return {@link String}
     */
    @GetMapping("/addToCartSuccessPage.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId, Model model) {
        CartItem item = cartService.fetchCartItem(skuId);
        model.addAttribute("item", item);
        return "success";
    }

}
