package cn.miozus.common.exception;

/**
 * 商业代码枚举
 *
 * @author miao
 * @date 2021/10/02
 */
public enum BizCodeEnum {
    UNKNOW_EXCEPTION(10000, "系统未知异常"),
    VALID_EXCEPTION(10001, "参数校验失败"),
    PUBLISH_EXCEPTION(11000, "商品上架异常");
    /**
     * 代码
     */
    private final int code;
    /**
     * 味精
     */
    private final String msg;

    BizCodeEnum(int code, String msg) {
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
