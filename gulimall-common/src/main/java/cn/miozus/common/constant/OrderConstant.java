package cn.miozus.common.constant;

import lombok.AllArgsConstructor;

/**
 * 订单常量
 *
 * @author miao
 * @date 2022/01/21
 */
public class OrderConstant {
    public static final String ORDER_USER_TOKEN_PREFIX = "order:token:";
    public static final double FRONT_BACK_PRICE_FLOAT_THRESHOLD = 0.01;

    @AllArgsConstructor
    public enum StatusEnum {
        /**
         * 订单状态
         */
        WAITING_PAY(0, "待付款"),
        WAITING_DELIVER(1, "待发货"),
        DELIVERED(2, "已发货"),
        FINISHED(3, "已完成"),
        CLOSED(4, "已关闭"),
        INVALID(5, "无效订单");

        public final int code;
        public final String msg;

    }

}