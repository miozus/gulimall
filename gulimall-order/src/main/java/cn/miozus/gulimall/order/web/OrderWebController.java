package cn.miozus.gulimall.order.web;

import cn.miozus.common.exception.NoStockException;
import cn.miozus.gulimall.order.service.OrderService;
import cn.miozus.gulimall.order.vo.OrderConfirmVo;
import cn.miozus.gulimall.order.vo.OrderSubmitRespVo;
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
        OrderSubmitRespVo respVo = null;
        String msg = "下单失败，";
        Integer code = null;
        try {
            respVo = orderService.submitOrder(orderSubmitVo);
            code = respVo.getCode();
            if (code != 0) {
                switch (code) {
                    case 1:
                        msg += "防重令牌校验失败，请重新提交";
                        break;
                    case 2:
                        msg += "价格发生变化，超过误差阈值";
                        break;
                    default:
                }
                redirectAttributes.addFlashAttribute("msg", msg);
                return "redirect:http://order.gulimall.com/toTrade";
            }
            msg = "提交成功";
        } catch (NoStockException e) {
            msg = "库存锁定失败，因为商品库存不足";
            redirectAttributes.addFlashAttribute("msg", msg);
            return "redirect:http://order.gulimall.com/toTrade";
        }
        log.info("📤 BizCode {} : {} ", code, msg);
        model.addAttribute("orderSubmitResp", respVo);
        return "pay";
    }


}

