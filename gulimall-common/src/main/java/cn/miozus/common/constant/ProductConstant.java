package cn.miozus.common.constant;

/**
 * 产品常量
 *
 * @author miao
 * @date 2021/09/08
 */
public class ProductConstant {

    /**
     * 属性表常量
     */
    public enum AttrEnum {
        ATTR_TYPE_BASE(1, "基本属性"),
        ATTR_TYPE_SALE(0, "销售属性");

        final private int code;
        final private String msg;

        AttrEnum(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }


        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}