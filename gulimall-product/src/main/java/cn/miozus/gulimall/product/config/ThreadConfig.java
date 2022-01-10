package cn.miozus.gulimall.product.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置
 *
 * 1.EnableConfigurationProperties(ThreadPoolConfigProperties.class)
 * 2.将此类以注解方式放入容器中 Component，二选一
 * 然后 ThreadPoolConfigProperties pool 作为参数，放在配置方法的参数调用
 * 3. nacos 配置，导入依赖，注册中心 👍
 *
 * @author miao
 * @date 2021/12/28
 */
@Configuration
public class ThreadConfig {

    @Value("${gulimall.thread.core-size}")
    private Integer corePoolSize;
    @Value("${gulimall.thread.max-size}")
    private Integer maximumPoolSize;
    @Value("${gulimall.thread.keep-alive-time}")
    private Integer keepAliveTime;

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {

        return new ThreadPoolExecutor(corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );

    }
}
