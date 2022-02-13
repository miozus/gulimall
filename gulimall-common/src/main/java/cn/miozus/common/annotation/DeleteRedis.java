package cn.miozus.common.annotation;

import java.lang.annotation.*;

/**
 * 同步删除缓存信息
 *
 * @author miao
 * @date 2022/02/08
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DeleteRedis {

    /**
     * 更新字段的注释，默认为空，无其他作用
     */
    String value() default "";
}
