package cn.miozus.gulimall.common.enume;

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
 * 16: 中间件
 * 21: 库存
 *
 * @author miao
 * @date 2021/10/02
 */
public enum BizCodeEnum {
    /** 微服务通用错误码 */
    UNKNOWN_EXCEPTION(10000, "系统未知异常"),
    VALID_EXCEPTION(10001, "参数校验异常"),
    CONNECT_EXCEPTION(10002, "服务通信异常"),
    SMS_CODE_FREQUENTLY_EXCEPTION(10002, "短信验证码获取频率太高，稍后再试"),
    SMS_CODE_INVALID_EXCEPTION(10003, "验证码错误"),
    PUBLISH_EXCEPTION(11000, "商品上架异常"),
    RECEIVER_NEEDED_EXCEPTION(12001, "收货地址不能为空"),
    PRICE_SUBTRACT_ACCURACY_OVER_THRESHOLD(12002, "价格发生变化，超过误差阈值"),
    CART_ITEM_EMPTY_EXCEPTION(13001, "购物车是空的，不能去支付"),
    CART_ITEM_NOT_EXIST_EXCEPTION(13002, "无勾选的商品，不能去支付"),
    PHONE_ALREADY_EXISTS_EXCEPTION(15001,"手机号已存在"),
    USERNAME_ALREADY_EXISTS_EXCEPTION(15002,"用户名已存在"),
    USERNAME_OR_PASSWORD_INVALID_EXCEPTION(15003,"用户名或密码错误"),
    OAUTH_NOT_BIND_EXCEPTION(15004,"授权登录未绑定用户"),
    REDIS_TOKEN_INVALID_EXCEPTION(16001,"令牌校验失败，请勿重复提交"),
    REDIS_CONDITION_DEPEND_PREVIEW_EXCEPTION(16002,"前置条件出异常，操作缓存失败"),
    FEIGN_READ_TIMEOUT_EXCEPTION(16003,"远程查询超时，网络故障"),
    NO_STOCK_EXCEPTION(21000,"库存不足");

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
