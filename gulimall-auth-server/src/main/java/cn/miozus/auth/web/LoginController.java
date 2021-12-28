package cn.miozus.auth.web;


import cn.miozus.auth.feign.PluginsService;
import cn.miozus.common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;

/**
 * 登录控制器
 *
 * SpringMVC viewcontroller: 将请求和页面映射，节省多个空方法
 *
 *
 * @author miao
 * @date 2021/12/28
 */
@Controller
public class LoginController {

    @Autowired
    PluginsService pluginsService;

    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone){
        String code = UUID.randomUUID().toString().substring(0, 5);
        pluginsService.sendCode(phone, code);
        return R.ok();
    }

    //public String login() {
    //    return "login";
    //}
    //
    //@GetMapping("/register.html")
    //public String register() {
    //    return "register";
    //}

}
