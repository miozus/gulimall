package cn.miozus.gulimall.auth.controller;


import cn.miozus.gulimall.common.annotation.TableInterceptor;
import cn.miozus.gulimall.common.constant.AuthServerConstant;
import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.auth.service.AuthService;
import cn.miozus.gulimall.auth.vo.UserLoginVo;
import cn.miozus.gulimall.auth.vo.UserRegisterVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.Objects;

/**
 * 登录控制器
 * <p>
 * SpringMVC viewController: 将请求和页面映射，节省多个空方法
 *
 * @author miao
 * @date 2021/12/28
 */
@Controller
@Slf4j
public class LoginController {

    @Autowired
    AuthService authService;

    /**
     * 发送短信验证码
     * 1.接口防刷: 60秒内弹窗提示频率过高
     * 2.验证码的再次校验:redis : key=prefix:phone_time, value=code;
     * 将第二个计时器放在第三方（权力从网页刷新转移），优先查第三方，判断本地系统时间的差值
     * API: 阿里云云市场【三网合一短信接口 - 支持协号转网】短信接口 短信验证码发送接口
     * 这个接口巨难用，经常失灵：线下测试从 redis 查看验证码
     * <p>
     * 校验验证码
     * 令牌机制：每次校验取出后删除旧的验证码
     * 防御写法：空，输错，都抛出，后面调用真正的远程服务校验逻辑
     *
     * @param phone 电话
     * @return {@link R}
     */
    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone) {
        return authService.sendCode(phone, "code");
    }

    /**
     * 注册
     * 成功时返回登录页，失败时红字显示参数校验结果
     * <p>
     * 🐞 Request method 'POST' not : 用户注册[POST] > forward 转发（路径映射默认[GET]） > MVC.model 返回渲染视图（但表单重复提交）
     * > 重定向到简单路由（但请求域中的变量访问不到了） > MVC.redirectAttributes 模拟重定向视图携带数据（写完整URL） ✅
     * <p>
     * TODO：分布式重定向问题，利用 session 作为媒介，只要跳转下一个页面，会删除媒介
     * TODO: 验证码错误返回后，表单数据被清空了， redirectAttributes （size=0）
     *
     * @param vo                 用户注册提供的数据
     * @param result             参数校验结果
     * @param redirectAttributes 重定向属性
     * @param session            会话
     * @return {@link R}
     */
    @PostMapping("/register")
    @TableInterceptor(value = "注册表单", remainUrl = "redirect:http://auth.gulimall.com/register.html")
    public String register(@Valid UserRegisterVo vo, BindingResult result, RedirectAttributes redirectAttributes, HttpSession session) {
        return "redirect:http://auth.gulimall.com/login.html";
    }


    /**
     * 登录
     * <p>
     * 注册登录和社交登录都返回成员实体，成功时，可取出放入会话
     * <p>
     * 四个必选参数：
     *
     * @param vo                 视图模型，提交的KV（而非JSON，不用RequestBody）
     * @param redirectAttributes 重定向属性
     * @param session            会话
     * @param bindingResult      绑定结果
     * @return {@link R}
     */
    @PostMapping("/login")
    @TableInterceptor(value = "登录表单", remainUrl = "redirect:http://auth.gulimall.com/login.html")
    public String login(@Valid UserLoginVo vo, BindingResult bindingResult, RedirectAttributes redirectAttributes, HttpSession session) {
        return "redirect:http://gulimall.com";
    }

    /**
     * 登录页面
     */
    @RequestMapping("/login.html")
    public String loginPage(HttpSession session) {
        Object data = session.getAttribute(AuthServerConstant.LOGIN_USER);
        return (Objects.nonNull(data)) ? "redirect:http://gulimall.com" : "login";

    }

}
