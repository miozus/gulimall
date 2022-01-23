package cn.miozus.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * 锁库存的需要的字段
 *
 * @author miao
 * @date 2022/01/22
 */
@Data
public class WareSkuLockVo {
    /** 订单流水号 */
    private String orderSn;
    /** 需要锁住的所有库存信息 */
    private List<OrderItemVo> orderItems;
}
