package cn.miozus.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * gulimall订单应用程序
 *
 * @author miao
 * @date 2022/01/13
 */
@EnableDiscoveryClient
@SpringBootApplication
@EnableRabbit
@EnableRedisHttpSession
@EnableFeignClients
@EnableCircuitBreaker
@EnableTransactionManagement(order = 0)
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }

}
