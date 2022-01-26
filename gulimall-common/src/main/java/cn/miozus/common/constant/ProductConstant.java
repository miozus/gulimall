package cn.miozus.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 产品常量
 *
 * @author miao
 * @date 2021/09/08
 */
public class ProductConstant {

    @Getter
    @AllArgsConstructor
    public enum AttrEnum {
        /**
         * 属性表常量
         */
        ATTR_TYPE_BASE(1, "基本属性"),
        ATTR_TYPE_SALE(0, "销售属性"),
        SEARCH_TYPE_ENABLE(1, "销售属性"),
        SEARCH_TYPE_DISABLE(0, "销售属性");

        private final int code;
        private final String msg;

    }

    @Getter
    @AllArgsConstructor
    public enum PublishStatusEnum {
        /**
         * 发布状态枚举
         */
        NEW_SPU(0, "新建"),
        SPU_UP(1, "上架"),
        SPU_DOWN(2, "下架");

        private final int code;
        private final String msg;
    }

}
