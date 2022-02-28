package cn.miozus.gulimall.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 同步更新缓存信息
 *
 * @author miao
 * @date 2022/02/08
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PutRedis {

    /** comment */
    String value() default "";

    /** keep alive time */
    int time() default 3;

    /** minute */
    TimeUnit unit() default TimeUnit.MINUTES;

    /** 键 */
    String key() default  "";
}
