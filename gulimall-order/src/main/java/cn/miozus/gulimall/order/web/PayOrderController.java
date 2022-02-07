package cn.miozus.gulimall.order.web;

import cn.miozus.gulimall.order.config.AlipayTemplate;
import cn.miozus.gulimall.order.service.OrderService;
import cn.miozus.gulimall.order.vo.PayVo;
import com.alipay.api.AlipayApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 支付方式控制台
 *
 * @author miao
 * @date 2022/01/26
 */
@Controller
@Slf4j
public class PayOrderController {

    @Autowired
    AlipayTemplate alipayTemplate;
    @Autowired
    OrderService orderService;

    @ResponseBody
    @GetMapping(value="/aliPay", produces="text/html")
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
        PayVo payVo = orderService.getOrderPay(orderSn);
        String pay = alipayTemplate.pay(payVo);
        log.info("💴 pay {} ", pay);
        return pay;
    }
}
