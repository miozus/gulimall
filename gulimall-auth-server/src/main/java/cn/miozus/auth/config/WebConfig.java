package cn.miozus.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 网络配置
 * 配置简单的自动控制器，这些控制器预先配置了响应状态代码和/或视图以呈现响应正文。
 * 这在不需要自定义控制器逻辑的情况下非常有用
 *
 * @author miao
 * @date 2021/12/28
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login.html").setViewName("login");
        registry.addViewController("/register.html").setViewName("register");
    }
}
