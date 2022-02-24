package cn.miozus.gulimall.auth.aspect;

import cn.miozus.common.annotation.Idempotent;
import cn.miozus.common.constant.AuthServerConstant;
import cn.miozus.common.enume.BizCodeEnum;
import cn.miozus.common.utils.R;
import cn.miozus.gulimall.auth.vo.UserRegisterVo;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * auth复述,方面
 *
 * @author miozus
 * @date 2022/02/22
 */
@Aspect
@Component
@Order(10)
public class AuthRedisAspect {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Around("@annotation(idempotent)")
    public Object before(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        Object primeParam = pjp.getArgs()[0];
        String clazz = primeParam.getClass().getSimpleName();
        switch (clazz) {
            case "String":
                return sendSmsCode(pjp, idempotent);
            case "UserRegisterVo":
                return checkSmsCode(primeParam);
            default:
        }

        return pjp.proceed();
    }

    private Object sendSmsCode(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        int keepAliveTime = idempotent.time();
        String redisKey = AuthServerConstant.SMS_CODE_TOKEN_PREFIX + pjp.getArgs()[0];
        String redisCode = redisTemplate.opsForValue().get(redisKey);
        if (StringUtils.isNotEmpty(redisCode)) {
            long redisTimeStamp = Long.parseLong(redisCode.split("_")[1]);
            long duration = System.currentTimeMillis() - redisTimeStamp;
            int oneMinute = (int) TimeUnit.MINUTES.toSeconds(1);
            if (duration < oneMinute) {
                return R.error(BizCodeEnum.SMS_CODE_FREQUENTLY_EXCEPTION);
            }
        }
        String code = UUID.randomUUID().toString().substring(0, 5) + "_" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(redisKey, code, keepAliveTime, TimeUnit.MINUTES);
        Object[] args = pjp.getArgs();
        args[1] = code;
        return pjp.proceed(args);
    }

    private Object checkSmsCode(Object primeParam) {
        UserRegisterVo vo = (UserRegisterVo) primeParam;
        String redisKey = AuthServerConstant.SMS_CODE_TOKEN_PREFIX + vo.getPhone();
        String redisCode = redisTemplate.opsForValue().get(redisKey);
        String code = vo.getCode();
        if (StringUtils.isEmpty(redisCode) || !code.equalsIgnoreCase(redisCode.split("_")[0])) {
            return R.error(BizCodeEnum.SMS_CODE_INVALID_EXCEPTION);
        }
        redisTemplate.delete(redisKey);
        return R.ok();
    }
}
