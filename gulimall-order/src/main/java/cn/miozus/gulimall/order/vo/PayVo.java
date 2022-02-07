package cn.miozus.gulimall.order.vo;

import lombok.Data;

/**
 * 支付签证官
 * 支付提交表单
 *
 * @author miao
 * @date 2022/01/26
 */
@Data
public class PayVo {
    /** 商户订单号 必填 */
    private String outTradeNo;
    /** 订单名称 必填 */
    private String subject;
    /** 付款金额 必填 */
    private String totalAmount;
    /** 商品描述 可空 */
    private String body;
}
