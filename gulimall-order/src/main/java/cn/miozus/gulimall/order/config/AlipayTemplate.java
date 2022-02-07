package cn.miozus.gulimall.order.config;

import cn.miozus.gulimall.order.vo.PayVo;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
@Slf4j
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "2021000117624697";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCXlhB1/6Qcem0CntBStEwnGsEkJLR9SDHU0/w1dOkIv+sOM/3ZLYhXUN7JNGC9GqOK41RT9UjO9OjALj4fQDxkTMS7DpQYkp9Oul7BZvIzOyuXn+rJJMQfKE0l7txyoQP8bS9JPFqE1IDijqiZKzaoS4r5aLgAgDvo2Q/lDE6DCJGKSTz58R5FuknhprF/mHPNAIQ55ECzfdwovo786U3r8PIKvgLV3DOW1B9tbiRfvfZfZyTUWGzQKuSKOP+TP7GemuFvByNWQmmYqHm5+RxXG1I6D1ryL7S+vyOsTvFcDVsxFQ3zWR67PElVLte+NrxDA5/+5eK1KGJc7WpIo/F/AgMBAAECggEAFIqoZmUjJxzMAuvkjSCTpUTx4WT79HJBFnc3mULjhWUEhGM1RiXucO6rkhZ/+YBPozWVKt91Y1464DgkAPYiePESQ8sA9KeB4RhOWOULczfUe3KXTXSnAxXiBn7s7re1I5LkMod0OMXXRVxRqpf5iyrZhVzUenTmKXKovcBCL21W6ItHR4gouWNz7xF+miLf7QVS0h/2ZscTvx79BVKUr6U8eEMilgu4A0w9/ZkiAusAj3Sp4PVZ93kNTDznB1g9sNRZKLoB2Q19Fkbe3qykBPnai6cZtk5OGzwI5ItsM2OiS9RYs6InhzqVtChnP3OOFzNkfHgRIv22BlWJawm/kQKBgQDiuAuE3Y/dqo+v4ynSJjQPUblFWQpSL8F4CLUdHJ8bhf+5juAmAA0Hgf4vYE4zJ7zUwIilH74sFrUNh8E4d0UIJJl4aCqRKV6pp2Iz4FJI1Mo762NCIRq7t7CycWmzDHETDfwvI1GCR+JLKa4QlWUTmwNWlsF0/Oy8ch0JaAiZFwKBgQCrKexSIaRv+saYpzEKtqhyqrBvStllzWZ5y9hSayAVGtqW2C10VlVwqC9J57wD5XKTNYFTLkwqnsvN69WCyRkLDJUc5XKtljVom5PH1n61tuX0zCGjHDNt9RgQ/lpIWQMrWxYz/iehtvG7j7n8LIOrhFvYh6H5seyxw2SqE+lb2QKBgGzA1h203kb5gxVfBXeeBcj3bcT7Mj08VTTMEHzXducP/xw2kgUIbzeTvqdhLgHR5P21IZb7xBCID+9emGwKol2GmOuuJAUf8B23gh7aUv5GvH/DqQhWsukq8yKVzpsps6/tPHQsH/Q7oSxm4O1pjGO8LvzBMil5DeJTZuRVOBKfAoGAJBNY+NKYavWrJ/+NKZnoFQr+1uYqvfc10xPwyyZDA8++JK75nZyYs37vQJ90FEonBPnxAQwwB4eiowtC3CbwfVCwmP+PVSDkruCjnCoMWNZsz1S6/jryaAmRgftqIfeI4Hl5S4oU5lO9zW90nr7vHZZvGUs1C4DBx5HMRMCfnbkCgYAmEm0Rseg3oerSCVdu/MpEkbppJN9l+bQ8V9PSoEQ8YULAMqx6UfRHJ4cmYMtc1++ZVUjdOmYsHmKKjGeuFg2AlbN5RgksZ3fcQdtW2tZ0tIf1jXmPcDstcyOdafmc5COdtF9kVVmZy2JJhYno8+ENq9xRUp1z/pP7eoVv2V+Fyg==";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0yVdMYss5uMVPoaB3E5SQewUbhc6dvjylQ1t3HEyupCK+nhxn5w/vjnBbDBQXBV87QF31mrE0zunti3zdxaJQPJPo16+MbwYcxpUu9ky0bNl8wHTamNBlO3iY68+vBM0MITLMnNeDSXCVExu3INezkGJxNUThZO9TMsREfmVcqYKq1z2Oepp4xmHSynkpekd0UepNobiFihweZVvYmweVh05aHIuDqPzLFZo32Y6NnIQfC0wg23qvXTxYy9A5JywYTHBsqdMoLtjTyoU2cU0klOLUXscmUxbAIhKrCWud1DBm+AKNKgBXKdn1NDc75EXma7qKQbedAMf+2PFV0qF/QIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url = "http://cn-cd-dx-2.natfrp.cloud:46651/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url = "http://member.gulimall.com/memberOrder.html";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String outTradeNo = vo.getOutTradeNo();
        //付款金额，必填
        String totalAmount = vo.getTotalAmount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ outTradeNo +"\","
                + "\"total_amount\":\""+ totalAmount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        log.info("💴 result {} ", result);

        return result;

    }
}
