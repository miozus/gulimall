package cn.miozus.gulimall.common.aspect;

import cn.hutool.core.date.SystemClock;
import cn.hutool.json.JSONUtil;
import cn.miozus.gulimall.common.enume.SysLog;
import cn.miozus.gulimall.common.to.SysLogTo;
import cn.miozus.gulimall.common.utils.IpHelper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * syslog切面
 *
 * @author miozus
 * @date 2022/02/28
 */
@Component
@Aspect
@Slf4j
public class SysLogAspect {
    @Autowired
//    private SysLogService sysLogService;
//    private static Logger logger = LoggerFactory.getLogger(SysLogAspect.class);

    @Around("@annotation(sysLog)")
    public Object around(ProceedingJoinPoint joinPoint, SysLog sysLog) throws Throwable {
        long beginTime = SystemClock.now();
        //执行方法
        Object result = joinPoint.proceed();
        //执行时长(毫秒)
        long time = SystemClock.now() - beginTime;

        SysLogTo sysLogTo = new SysLogTo();
        if(sysLog != null){
            //注解上的描述
            sysLogTo.setOperation(sysLog.value());
        }

        //请求的方法名
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        sysLogTo.setMethod(className + "." + methodName + "()");

        //请求的参数
        Object[] args = joinPoint.getArgs();
        String params = JSONUtil.toJsonStr(args[0]);
        sysLogTo.setParams(params);

        //设置IP地址
        sysLogTo.setIp(IpHelper.getIpAddr());
        //用户名：需要 Shiro 授权框架/Spring-Security 支持
//        String username = SecurityUtils.getSysUser().getUsername();
//        sysLogEntity.setUsername(username);
        sysLogTo.setTime(time);
        sysLogTo.setCreateDate(new Date());
        //保存系统日志
        log.info("sysLogEntity {} ", sysLogTo);
//        sysLogService.save(sysLogTo);
        return result;
    }

}

