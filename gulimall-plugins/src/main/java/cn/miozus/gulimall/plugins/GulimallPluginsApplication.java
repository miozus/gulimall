package cn.miozus.gulimall.plugins;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class GulimallPluginsApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallPluginsApplication.class, args);
    }

}
