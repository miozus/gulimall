package cn.miozus.gulimall.order.listener;

import cn.miozus.gulimall.order.config.AlipayTemplate;
import cn.miozus.gulimall.order.service.OrderService;
import cn.miozus.gulimall.order.vo.PayAsyncVo;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单通知侦听器
 *
 * @author miao
 * @date 2022/02/07
 */
@RestController
@Slf4j
public class OrderNotifyListener {

    @Autowired
    OrderService orderService;
    @Autowired
    AlipayTemplate alipayTemplate;

    @PostMapping("/notify/pay")
    public String handlerAliPay(HttpServletRequest request, PayAsyncVo vo) throws AlipayApiException, UnsupportedEncodingException {
        // 校验参数
        // 校验签名
        Map<String, String> params = getParams(request);
        boolean signVerified = AlipaySignature.verifyV1(params, alipayTemplate.getAlipayPublicKey(),
                alipayTemplate.getCharset(), alipayTemplate.getSignType());
        // 更新订单状态
        if (signVerified) {
            orderService.handlePayResult(vo);
            log.info("验签成功");
            return "success";
        } else {
            log.info("支付异步通知验签失败");
            return "fail";
        }
    }

    private Map<String, String> getParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>(16);
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (String name : parameterMap.keySet()) {
            String[] values = parameterMap.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用（否则出现乱码）
            // valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }
        return params;
    }
}
