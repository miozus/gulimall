package cn.miozus.gulimall.cart.config;

import cn.miozus.gulimall.cart.interceptor.CartInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 网络配置
 * 拦截所有请求，跨域共享线程中变量
 *
 * @author miao
 * @date 2021/12/28
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 添加拦截器: 拦截所有请求
     *
     * @param registry 注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CartInterceptor()).addPathPatterns("/**");
    }
}
