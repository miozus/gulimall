package cn.miozus.gulimall.common.annotation;

import java.lang.annotation.*;

/**
 * 消息队列切面
 *
 * @author miozus
 * @date 2022/02/17
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PostRabbitMq {

    /** 参数 key */
    String key() default "";

    /** 注释或留空 */
    String value() default "";
}
