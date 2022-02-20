package cn.miozus.gulimall.order.web;

import cn.miozus.common.exception.GuliMallBindException;
import cn.miozus.gulimall.order.entity.OrderEntity;
import cn.miozus.gulimall.order.service.OrderService;
import cn.miozus.gulimall.order.vo.OrderConfirmVo;
import cn.miozus.gulimall.order.vo.OrderSubmitVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 订单控制台
 *
 * @author miao
 * @date 2022/01/14
 */
@Controller
@Slf4j
public class OrderWebController {

    @Autowired
    OrderService orderService;

    @GetMapping("/{page}.html")
    public String renderPage(@PathVariable("page") String page) {
        return page;
    }


    @GetMapping("/toTrade")
    public String toTrade(Model model) {
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmVo", confirmVo);
        return "confirm";
    }

    @PostMapping("/submit")
    public String submitOrder(OrderSubmitVo orderSubmitVo, Model model, RedirectAttributes redirectAttributes) {
        try {
            OrderEntity resp = orderService.submitOrder(orderSubmitVo);
            log.info("📤 订单提交成功:" + resp.getOrderSn());
            model.addAttribute("orderSubmitResp", resp);
            return "pay";
        } catch (GuliMallBindException e) {
            log.info("🐞 {} : {}", e.getBizCode(), e.getMessage());
            redirectAttributes.addFlashAttribute("msg", "下单失败，" + e.getMessage());
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }


}

