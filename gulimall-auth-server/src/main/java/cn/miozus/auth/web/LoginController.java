package cn.miozus.auth.web;


import cn.miozus.auth.feign.MemberFeignService;
import cn.miozus.auth.feign.PluginsFeignService;
import cn.miozus.auth.vo.UserLoginVo;
import cn.miozus.auth.vo.UserRegisterVo;
import cn.miozus.common.constant.AuthServerConstant;
import cn.miozus.common.exception.BizCodeEnum;
import cn.miozus.common.utils.R;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 登录控制器
 * <p>
 * SpringMVC viewController: 将请求和页面映射，节省多个空方法
 *
 * @author miao
 * @date 2021/12/28
 */
@Controller
public class LoginController {

    @Autowired
    PluginsFeignService pluginsFeignService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    MemberFeignService memberFeignService;

    /**
     * 发送短信验证码
     * 1.接口防刷: 60秒内弹窗提示频率过高
     * 2.验证码的再次校验:redis : key=prefix:phone_time, value=code;
     * 将第二个计时器放在第三方（权力从网页刷新转移），优先查第三方，判断本地系统时间的差值
     * API: 阿里云云市场【三网合一短信接口 - 支持协号转网】短信接口 短信验证码发送接口
     * 经常失灵：线下测试从 redis 查看验证码
     *
     * @param phone 电话
     * @return {@link R}
     */
    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone) {
        int keepAliveTime = 10;
        int codeAliveTime = 60 * 1000;
        String redisKey = AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone;
        String redisCode = redisTemplate.opsForValue().get(redisKey);
        if (StringUtils.isNotEmpty(redisCode)) {
            long redisTimeStamp = Long.parseLong(redisCode.split("_")[1]);
            long duration = System.currentTimeMillis() - redisTimeStamp;
            if (duration < codeAliveTime) {
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }
        String code = UUID.randomUUID().toString().substring(0, 5) + "_" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(redisKey, code, keepAliveTime, TimeUnit.MINUTES);
        // pluginsFeignService.sendCode(phone, code);

        return R.ok();
    }

    /**
     * 注册成功：返回首页/登录页
     * <p>
     * 🐞 Request method 'POST' not : 用户注册[POST] > forward 转发（路径映射默认[GET]） > MVC.model 返回渲染视图（但表单重复提交）
     * > 重定向到简单路由（但请求域中的变量访问不到了） > MVC.redirectAttributes 模拟重定向视图携带数据（写完整URL） ✅
     * <p>
     * TODO：分布式重定向问题，利用 session 作为媒介，只要跳转下一个页面，会删除媒介
     *
     * @return {@link String}
     */
    @PostMapping("/register")
    public String register(@Valid UserRegisterVo vo, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream().collect(
                    Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)
            );
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/register.html";
        }
        return verifyCodeValid(vo, redirectAttributes);
    }


    /**
     * 登录
     *
     * @param vo                 视图模型，提交的KV（而非JSON，不用RequestBody）
     * @param redirectAttributes 重定向属性
     * @return {@link String}
     */
    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes) {
        R r = memberFeignService.login(vo);
        if (r.getCode() != 0) {
            Map<String, String> errors = new HashMap<>(1);
            errors.put("msg", r.getData("msg", new TypeReference<String>() {
            }));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
        return "redirect:http://gulimall.com";
    }

    /**
     * 校验验证码
     * 令牌机制：每次校验取出后删除旧的验证码
     * 防御写法：空，输错，都抛出，后面调用真正的远程服务校验逻辑
     *
     * @param vo                 签证官
     * @param redirectAttributes 重定向属性
     * @return {@link String}
     */
    private String verifyCodeValid(UserRegisterVo vo, RedirectAttributes redirectAttributes) {
        String code = vo.getCode();
        String redisKey = AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone();
        String redisCode = redisTemplate.opsForValue().get(redisKey);
        if (StringUtils.isEmpty(redisCode) || !code.equalsIgnoreCase(redisCode.split("_")[0])) {
            Map<String, String> errors = new HashMap<>(1);
            errors.put("code", "验证码错误");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/register.html";
        }
        redisTemplate.delete(redisKey);
        R r = memberFeignService.register(vo);
        if (r.getCode() != 0) {
            Map<String, String> errors = new HashMap<>(1);
            errors.put("msg", r.getData("msg", new TypeReference<String>() {
            }));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/register.html";
        }
        return "redirect:http://auth.gulimall.com/login.html";
    }

}