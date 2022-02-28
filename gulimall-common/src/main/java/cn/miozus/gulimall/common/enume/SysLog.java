package cn.miozus.gulimall.common.enume;

import java.lang.annotation.*;

/**
 * syslog
 *
 * @author miozus
 * @date 2022/02/28
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface SysLog {
    /**
     * 日志类型
     *
     * @return
     */
//    LogType type();

    /**
     * 日志描述
     *
     * @return
     */
    String value();
}
