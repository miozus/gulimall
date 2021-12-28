package cn.miozus.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * gulimall身份验证服务器应用程序
 *
 * @author miao
 * @date 2021/12/28
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class GulimallAuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallAuthServerApplication.class, args);
    }

}
