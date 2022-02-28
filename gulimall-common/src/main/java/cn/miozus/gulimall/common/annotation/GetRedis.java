package cn.miozus.gulimall.common.annotation;

import java.lang.annotation.*;

/**
 * 拉取最新新缓存信息
 *
 * @author miao
 * @date 2022/02/08
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GetRedis {

    /**
     * 查询的键，或者注释，默认为空
     */
    String value() default "";
}
