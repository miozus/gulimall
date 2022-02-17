package cn.miozus.common.enume;

/**
 * 错误代码枚举
 * 10:通用
 * - 001:参数格式校验
 * - 002:短信验证码频率太高
 * 11:商品
 * 12: 订单
 * 13: 购物车
 * 14: 物流
 * 15: 用户
 * 21: 库存
 *
 * @author miao
 * @date 2021/10/02
 */
public enum BizCodeEnum {
    /** 微服务通用错误码 */
    UNKNOWN_EXCEPTION(10000, "系统未知异常"),
    VALID_EXCEPTION(10001, "参数校验失败"),
    SMS_CODE_EXCEPTION(10002, "短信验证码获取频率太高，稍后再试"),
    PUBLISH_EXCEPTION(11000, "商品上架异常"),
    PHONE_ALREADY_EXISTS_EXCEPTION(15001,"手机号已存在"),
    USERNAME_ALREADY_EXISTS_EXCEPTION(15002,"用户名已存在"),
    USERNAME_OR_PASSWORD_INVALID_EXCEPTION(15003,"用户名或密码错误"),
    NO_STOCK_EXCEPTION(21000,"没有库存");

    private final int code;
    private final String msg;

    BizCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int value() {
        return this.code;
    }

    public String getMsg() {
        return msg;
    }

}
