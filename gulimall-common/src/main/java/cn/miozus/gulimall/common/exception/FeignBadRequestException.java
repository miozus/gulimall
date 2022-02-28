package cn.miozus.gulimall.common.exception;

import cn.miozus.gulimall.common.enume.BizCodeEnum;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import org.apache.http.HttpStatus;

/**
 * 微服务之间坏请求异常
 * 避免被熔断吞掉： 当抛出的异常是 HystrixBadRequestException（继承） 时，直接抛出异常，不再经过 fallback
 * @author miozus
 * @date 2022/02/27
 */
public class FeignBadRequestException extends HystrixBadRequestException {

    private static final long serialVersionUID = -8603452504568680525L;

    public Integer getCode() {
        return code;
    }

    private Integer code;

    public FeignBadRequestException(BizCodeEnum bizCode) {
        super(bizCode.getMsg());
        this.code = bizCode.value();
    }


    public FeignBadRequestException(String message) {
        super(message);
        this.code = HttpStatus.SC_BAD_REQUEST;
    }

    public FeignBadRequestException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public FeignBadRequestException(String code, String message) {
        super(message);
        this.code = Integer.valueOf(code);
    }
}