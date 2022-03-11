package cn.miozus.gulimall.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.password}")
    private String password;
    @Value("${spring.redis.port}")
    private String port;

    @Bean(destroyMethod="shutdown")
    public RedissonClient redisson() throws IOException {
        // 创建配置
        Config config = new Config();
        String url = "redis://" + host + ":" + port;

        config.useSingleServer().setAddress(url).setPassword(password);

        // 创建实例
        return Redisson.create(config);
    }}
