package cn.miozus.gulimall.common.config;

import cn.miozus.gulimall.common.enume.BizCodeEnum;
import cn.miozus.gulimall.common.exception.FeignBadRequestException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * 微服务之间客户端错误译码器：拦截feign客户端的错误信息，并将其转换为自定义异常
 *
 * @author miozus
 * @date 2022/02/27
 */
@Slf4j
@Configuration
public class FeignClientErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        if (isRespError(response)) {
            FeignBadRequestException internalApiException = new FeignBadRequestException(BizCodeEnum.CONNECT_EXCEPTION);
            internalApiException = convertFeignClientErrorException(response, internalApiException);
            return internalApiException;
        }
        return new FeignBadRequestException(BizCodeEnum.UNKNOWN_EXCEPTION);
    }

    /**
     * 因为与被调用方约定当状态码为 SERVICE_UNAVAILABLE 的时候视为被调用方主动抛出的异常
     */
    private boolean isRespError(Response response) {
        return response.status() != HttpStatus.OK.value() && response.status() == HttpStatus.SERVICE_UNAVAILABLE.value();
    }

    private FeignBadRequestException convertFeignClientErrorException(Response response, FeignBadRequestException internalApiException) {
        String errorContent;
        try {
            errorContent = Util.toString(response.body().asReader(Charset.availableCharsets().get("UTF-8")));
            if (Strings.isNotBlank(errorContent)) {
                errorContent = errorContent.replace("\t", "").replace("\r", "").replace("\n", "");
            }
            JSONObject jsonObject = JSON.parseObject(errorContent);
            if (jsonObject != null) {
                String code = jsonObject.getString("code");
                String message = jsonObject.getString("message");
                if (Strings.isNotBlank(code) && Strings.isNotBlank(message)) {
                    internalApiException = new FeignBadRequestException(code, message);
                }
            }
        } catch (IOException e) {
            log.error("Feign 处理异常错误：FeignClientErrorDecoder decode error", e);
        }
        return internalApiException;
    }
}
