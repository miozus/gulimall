package cn.miozus.gulimall.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 幂等：Redis 校验 Token
 *
 * @author miozus
 * @date 2022/02/17
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /** comment */
    String value() default "";

    /** keep alive time */
    int time() default 3;

    /** minute */
    TimeUnit unit() default TimeUnit.MINUTES;

    /** 键 */
    String key() default  "";
}
