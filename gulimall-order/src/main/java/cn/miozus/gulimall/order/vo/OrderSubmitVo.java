package cn.miozus.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单提交（前端）
 *
 * 商品信息直接从购物车获取，不用重复提交了
 * 可添加：优惠、发票
 *
 * @author miao
 * @date 2022/01/21
 */
@Data
public class OrderSubmitVo {
    private String addrId;
    private String payType;
    private String orderToken;
    private BigDecimal payPrice;
    /** 备注 */
    private String note;
    /** 登录用户序号 */
    private Long uid;

}
