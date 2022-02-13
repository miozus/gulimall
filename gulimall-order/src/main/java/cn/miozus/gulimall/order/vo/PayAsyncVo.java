package cn.miozus.gulimall.order.vo;

import lombok.Data;

import java.util.Date;

/**
 * 支付异步签证官
 *
 * @author miao
 * @date 2022/02/08
 */
@Data
public class PayAsyncVo {

    private String gmtCreate;
    private String charset;
    private String gmtPayment;
    private Date notifyTime;
    private String subject;
    private String sign;
    /**
     * 支付者的id
     */
    private String buyerId;
    /**
     * 订单的信息
     */
    private String body;
    /**
     * 支付金额
     */
    private String invoiceAmount;
    private String version;
    /**
     * 通知id
     */
    private String notifyId;
    private String fundBillList;
    /**
     * 通知类型； trade_status_sync
     */
    private String notifyType;
    /**
     * 订单号
     */
    private String outTradeNo;
    /**
     * 支付的总额
     */
    private String totalAmount;
    /**
     * 交易状态  TRADE_SUCCESS
     */
    private String tradeStatus;
    /**
     * 流水号
     */
    private String tradeNo;
    private String authAppId;
    /**
     * 商家收到的款
     */
    private String receiptAmount;
    private String pointAmount;
    /**
     * 应用id
     */
    private String appId;
    /**
     * 最终支付的金额
     */
    private String buyerPayAmount;
    /**
     * 签名类型
     */
    private String signType;
    /**
     * 商家的id
     */
    private String sellerId;

}
