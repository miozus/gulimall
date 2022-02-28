package cn.miozus.gulimall.common.config;

import cn.miozus.gulimall.common.exception.FeignBadRequestException;
import cn.miozus.gulimall.common.exception.GuliMallBindException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常：默认异常处理器
 *
 * @author miozus
 * @date 2022/02/17
 */
@Controller
@RestControllerAdvice
public class DefaultExceptionHandlerConfig {


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

}
