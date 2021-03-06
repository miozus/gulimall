package cn.miozus.gulimall.product.exception;

import cn.miozus.gulimall.common.enume.BizCodeEnum;
import cn.miozus.gulimall.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * gulimall控制器集中处理所有异常
 *
 * @author miao
 * @date 2021/09/01
 */
@Slf4j
@RestControllerAdvice(basePackages = "cn.miozus.gulimall.product.controller")
public class GulimallExceptionControllerAdvice {
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R handleValidException(MethodArgumentNotValidException e) {
        log.error("数据校验出问题{}，异常类型：{}", e.getMessage(), e.getClass());
        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> errorMap = new HashMap<>();
        bindingResult.getFieldErrors().forEach((filedError) -> {
            errorMap.put(filedError.getField(), filedError.getDefaultMessage());
        });
        return R.error(BizCodeEnum.VALID_EXCEPTION.value(), BizCodeEnum.VALID_EXCEPTION.getMsg()).put("data", errorMap);
    }


    /**
     * 兜底：处理异常
     *
     * @return {@link R}
     */
    @ExceptionHandler(Throwable.class)
    public R handleException(Throwable throwable) {
        log.error("错误：{}", throwable);

        return R.error(BizCodeEnum.UNKNOWN_EXCEPTION.value(), BizCodeEnum.UNKNOWN_EXCEPTION.getMsg());
    }
}
