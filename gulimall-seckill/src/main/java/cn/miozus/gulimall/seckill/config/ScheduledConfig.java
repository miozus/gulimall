package cn.miozus.gulimall.seckill.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启定时任务配置
 *
 * @author miozus
 * @date 2022/03/11
 */
@EnableScheduling
@EnableAsync
@Configuration
public class ScheduledConfig {
    // annotation works
}
