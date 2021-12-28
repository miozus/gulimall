package cn.miozus.gulimall.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 线程池配置属性绑定
 * thread pool config properties
 *
 * @author miao
 * @date 2021/12/28
 */
@ConfigurationProperties(prefix = "gulimall.thread")
@Component
@Data
public class ThreadPoolConfigProperties {

    Integer corePoolSize = 20;
    Integer maximumPoolSize = 200;
    Integer keepAliveTime = 10;


}
