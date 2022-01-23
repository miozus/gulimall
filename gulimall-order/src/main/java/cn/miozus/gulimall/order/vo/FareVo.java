package cn.miozus.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 费用
 *
 * @author miao
 * @date 2022/01/19
 */
@Data
public class FareVo {
    private MemberReceiveAddressVo address;
    private BigDecimal fare;
}
