package cn.miozus.gulimall.order.config;

import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * 缓存配置
 *
 * @author miao
 * @date 2021/10/22
 */
@EnableConfigurationProperties(CacheProperties.class)
@Configuration
@EnableCaching
public class RedisCacheConfig {
    /**
     * 缓存配置
     * @annotation @EnableConfigurationProperties(CacheProperties.class) 容器中注入东西
     *    启用方式（可选）
     *      * @Autowired 声明注入
     *      * 方法就是给容器中放东西 ，传的所有参数，都会从容器中确定
     *
     * @return {@link RedisCacheConfiguration}
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration(CacheProperties cacheProperties) {
        // 链式调用，每次会生成新值，覆盖旧的 写法
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        // 全局配置：键值反序列化
        config = config
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));
        // 自定义配置：
        CacheProperties.Redis redisProperties = cacheProperties.getRedis();
        // 设置失效时间
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        // 缓存键，有无前缀，[cacheNames]::value ⇒ [prefix]::value
        if (redisProperties.getKeyPrefix() != null) {
            config = config.prefixCacheNameWith(redisProperties.getKeyPrefix());
        }
        // 要不要缓存空数据
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        // 是不是缓存键前缀
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        return config;
    }
}
