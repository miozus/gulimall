package cn.miozus.gulimall.order.config;

import cn.miozus.gulimall.order.vo.PayVo;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 阿里SDK
 */
@Getter
@Slf4j
@Configuration
public class AlipayTemplate {

    /**
     * 在支付宝创建的应用的id
     */
    @Value("${pay.alipay.appId}")
    private String appId;
    /**
     * 商户私钥，您的PKCS8格式RSA2私钥
     */
    @Value("${pay.alipay.merchantPrivateKey}")
    private String merchantPrivateKey;
    /**
     * 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
     */
    @Value("${pay.alipay.alipayPublicKey}")
    private String alipayPublicKey;

    /**
     * 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
     * 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息: 每次启动是随机的域名
     */
    @Value("${pay.alipay.notifyUrl}")
    private String notifyUrl;

    /**
     * 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
     * 同步通知，支付成功，一般跳转到成功页
     */
    @Value("${pay.alipay.returnUrl}")
    private String returnUrl;

    /**
     * 签名方式
     */
    @Value("${pay.alipay.signType}")
    private String signType;

    /**
     * 字符编码格式
     */
    @Value("${pay.alipay.charset}")
    private String charset;

    /**
     * 支付宝网关； https://openapi.alipaydev.com/gateway.do
     */
    @Value("${pay.alipay.gatewayUrl}")
    private String gatewayUrl;

    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(gatewayUrl, appId, merchantPrivateKey, "json", charset, alipayPublicKey, signType);
    }

    public String pay(PayVo vo) throws AlipayApiException {

        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = alipayClient();

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(returnUrl);
        alipayRequest.setNotifyUrl(notifyUrl);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String outTradeNo = vo.getOutTradeNo();
        //付款金额，必填
        String totalAmount = vo.getTotalAmount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\"" + outTradeNo + "\","
                + "\"total_amount\":\"" + totalAmount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"body\":\"" + body + "\","
                + "\"timeout_express\":\"15m\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        // 会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        log.info("💴 AliResponse: {} ", result);

        return result;

    }
}
