package cn.miozus.gulimall.order.web;

import cn.miozus.gulimall.order.service.OrderService;
import cn.miozus.gulimall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 你好控制器
 *
 * @author miao
 * @date 2022/01/14
 */
@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    @GetMapping("/{page}.html")
    public String renderPage(@PathVariable("page") String page){
        return page;

    }

    @GetMapping("/toTrade")
    public String toTrade(Model model){
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmVo", confirmVo);
        return "confirm";
    }


}

