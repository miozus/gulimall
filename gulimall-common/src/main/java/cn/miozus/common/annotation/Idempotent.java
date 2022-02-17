package cn.miozus.common.annotation;

import java.lang.annotation.*;

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

    /** 校验 key */
    String key() default "";

    /** 注释或留空 */
    String value() default "";
}
