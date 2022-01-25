package cn.miozus.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存常数
 *
 * @author miao
 * @date 2022/01/24
 */
public class WareConstant {

    @AllArgsConstructor
    @Getter
    public enum PurchaseStatusEnum {
        /**
         * 购买状态
         */
        CREATED(0, "新建"),
        ASSIGNED(1, "已分配"),
        RECEIVED(2, "已领取"),
        FINISHED(3, "已完成"),
        HASERROR(4, "有异常");

        private final int code;
        private final String msg;

    }

    @Getter
    @AllArgsConstructor
    public enum PurchaseDetailStatusEnum {
        /**
         * 购买细节状态
         */
        CREATED(0, "新建"),
        ASSIGNED(1, "已分配"),
        BUYING(2, "正在采购"),
        FINISHED(3, "已完成"),
        HASERROR(4, "采购失败");

        private final int code;
        private final String msg;
    }

    @Getter
    @AllArgsConstructor
    public enum StockLockedStatusEnum {
        /**]
         * 锁库存状态
         */
        LOCKED(1, "已锁定"),
        UNLOCKED(2, "已解锁"),
        SUBTRACTED(3, "扣减");

        private final int code;
        private final String msg;
    }

}
