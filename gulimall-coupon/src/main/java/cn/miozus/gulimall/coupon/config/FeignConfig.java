package cn.miozus.gulimall.coupon.config;

import com.alibaba.nacos.common.utils.Objects;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Feign 反洗钱拦截器
 *
 * 保留请求头：讲生产者携带的Cookie通过上下文保留器，继续传递给消费者
 *
 * @author miao
 * @date 2022/01/15
 */
@Configuration
public class FeignConfig {

    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor() {
        return template -> {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (Objects.nonNull(requestAttributes)) {
                HttpServletRequest publisherRequest = requestAttributes.getRequest();
                String cookie = publisherRequest.getHeader("Cookie");
                template.header("Cookie", cookie);
            }
        };

    }

}
