package cn.miozus.gulimall.common.enume;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 *
 * @author miao
 * @date 2022/01/21
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {
    /**
     * 订单状态
     */
    CREATE_NEW(0, "待付款"),
    PAYED(1, "已付款"),
    DELIVERED(2, "已发货"),
    RECEIVED(3, "已完成"),
    CANCELED(4, "已取消"),
    SERVICING(5, "售后中"),
    SERVICED(6, "售后完成");

    private final int code;
    private final String msg;
}
