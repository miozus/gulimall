package cn.miozus.common.exception;

import cn.miozus.common.enume.BizCodeEnum;
import org.apache.http.HttpStatus;

/**
 * 谷粒商城绑定异常
 *
 * @author miozus
 * @date 2022/02/17
 */
public class GuliMallBindException extends RuntimeException {

    private static final long serialVersionUID = 1271139314534724853L;


    /**
     * 微服务通用错误代码
     */
    private Integer bizCode;


    public GuliMallBindException(BizCodeEnum bizCode) {
        this.bizCode = bizCode.value();
    }

    public GuliMallBindException(String message, BizCodeEnum bizCode) {
        super(message);
        this.bizCode = bizCode.value();
    }

    public GuliMallBindException(String message) {
        super(message);
        this.bizCode = HttpStatus.SC_BAD_REQUEST;
    }

    public Integer getBizCode() {
        return bizCode;
    }
}
