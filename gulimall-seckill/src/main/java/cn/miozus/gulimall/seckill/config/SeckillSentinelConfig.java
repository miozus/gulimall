package cn.miozus.gulimall.seckill.config;

import com.alibaba.csp.sentinel.adapter.spring.webflux.callback.WebFluxCallbackManager;
import org.springframework.context.annotation.Configuration;

/**
 * 秒杀哨兵配置
 *
 * @author Miozus
 * @date 2022/03/30
 */
@Configuration
public class SeckillSentinelConfig {

    public SeckillSentinelConfig {
        WebFluxCallbackManager.setBlockHandler();
    }
}
