package cn.miozus.gulimall.seckill.exception;

import cn.hutool.json.JSONUtil;
import cn.miozus.gulimall.common.enume.BizCodeEnum;
import cn.miozus.gulimall.common.exception.FeignBadRequestException;
import cn.miozus.gulimall.common.exception.GuliMallBindException;
import cn.miozus.gulimall.common.utils.R;
import com.alibaba.csp.sentinel.slots.block.AbstractRule;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常：默认异常处理器（仅限单个微服务有效）
 *
 * @author miozus
 * @date 2022/02/17
 */
@Controller
@RestControllerAdvice
@Slf4j
@Order(0)
public class SeckillDefaultExceptionHandlerConfig {


    @ExceptionHandler(BindException.class)
    public ResponseEntity<String> bindExceptionHandler(BindException e){
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getBindingResult().getFieldErrors().get(0).getDefaultMessage());

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e){
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getBindingResult().getFieldErrors().get(0).getDefaultMessage());
    }

    @ExceptionHandler(GuliMallBindException.class)
    public ResponseEntity<String> unauthorizedExceptionHandler(GuliMallBindException e){
        e.printStackTrace();
        return ResponseEntity.status(e.getBizCode()).body(e.getMessage());
    }

    @ExceptionHandler(FeignBadRequestException.class)
    public ResponseEntity<String> feignBadRequestExceptionHandler(FeignBadRequestException e){
        e.printStackTrace();
        return ResponseEntity.status(e.getCode()).body(e.getMessage());
    }

    @ExceptionHandler(BlockException.class)
    @ResponseBody
    public String sentinelBlockHandler(BlockException e) {
        AbstractRule rule = e.getRule();
        log.info("Blocked by Sentinel: {}", rule.toString());
        R error = R.error(BizCodeEnum.TOO_MANY_REQUESTS);
        return JSONUtil.toJsonStr(error);
    }

}
