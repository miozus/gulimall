package cn.miozus.gulimall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * redisson配置
 * 所有调用对象入口: RedissonClient
 *
 * @author miao
 * @date 2021/10/10
 */
@Configuration
public class RedissonConfig {

    @Bean(destroyMethod="shutdown")
    public RedissonClient redisson() throws IOException {
        // 创建配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://39.108.228.2:6379").setPassword("Redis*78");

        // 创建实例
        return Redisson.create(config);
    }}