package cn.miozus.common.annotation;

import java.lang.annotation.*;

/**
 * 表单提交拦截器：拦截错误信息，都通过时才放行
 *
 * @author miozus
 * @date 2022/02/24
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TableInterceptor {
    String value() default "comment";

    /**
     * 成功跳转url
     */
    String returnUrl() default "";

    /**
     * 失败驻留url
     */
    String remainUrl() default "";

}
