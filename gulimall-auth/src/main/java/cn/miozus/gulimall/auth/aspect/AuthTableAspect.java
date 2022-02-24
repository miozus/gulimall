package cn.miozus.gulimall.auth.aspect;

import cn.miozus.common.annotation.TableInterceptor;
import cn.miozus.common.constant.AuthServerConstant;
import cn.miozus.common.utils.R;
import cn.miozus.common.vo.MemberRespVo;
import cn.miozus.gulimall.auth.service.AuthService;
import cn.miozus.gulimall.auth.vo.UserLoginVo;
import cn.miozus.gulimall.auth.vo.UserRegisterVo;
import com.alibaba.fastjson.TypeReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 身份验证表格切面
 *
 * @author miozus
 * @date 2022/02/24
 */
@Aspect
@Component
@Slf4j
@Order(99)
public class AuthTableAspect {

    @Autowired
    AuthService authService;


    /**
     * 检查表提交策略
     * 收集错误信息，提交前端渲染
     *
     * @param pjp              进程切点
     * @param tableInterceptor 表拦截器
     * @return {@link String}
     */
    @SneakyThrows
    @Around("@annotation(tableInterceptor)")
    public String checkTableSubmitStrategy(ProceedingJoinPoint pjp, TableInterceptor tableInterceptor) {
        Object[] args = pjp.getArgs();
        Object param = args[0];
        BindingResult bindingResult = (BindingResult) args[1];
        RedirectAttributes redirectAttributes = (RedirectAttributes) args[2];
        HttpSession session = (HttpSession) args[3];
        String returnUrl = pjp.proceed(args).toString();
        String remainUrl = tableInterceptor.remainUrl();
        String clazz = param.getClass().getSimpleName();

        Map<String, String> errors = new HashMap<>(4);
        if (bindingResult.hasErrors()) {
            errors = bindingResult.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            errors.put("msg", "格式有误");
            redirectAttributes.addFlashAttribute("errors", errors);
            return remainUrl;
        }
        try {
            R r = doProceedingMethodStrategy(param, clazz);
            if (r.getCode() == 0) {
                MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
                });
                if ("UserLoginVo".equals(clazz)) {
                    session.setAttribute(AuthServerConstant.LOGIN_USER, data);
                    log.info("登陆成功： data {}", data);
                }
                return returnUrl;
            } else {
                String msg = r.getData("msg", new TypeReference<String>() {
                });
                errors.put("msg", msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            errors.put("msg", e.getMessage());
        }
        redirectAttributes.addFlashAttribute("errors", errors);
        return remainUrl;
    }

    private R doProceedingMethodStrategy(Object param, String clazz) {
        R r;
        if ("UserLoginVo".equals(clazz)) {
            r = authService.login((UserLoginVo) param);
        } else {
            r = authService.register((UserRegisterVo) param);
        }
        return r;
    }
}
