package cn.miozus.gulimall.common.to.mq;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 秒杀订单
 *
 * @author Miozus
 * @date 2022/03/26
 */
@Data
public class SeckillOrderTo implements Serializable {

    private static final long serialVersionUID = -4587938338710071805L;

    /** 订单id */
    private  String orderSn;
    /** 活动场次id */
    private Long promotionSessionId;
    /** 商品id */
    private Long skuId;
    /** 秒杀价格 */
    private BigDecimal seckillPrice;
    /** 购买数量 */
    private Integer num;
    /** 会员id */
    private Long memberId;
}
