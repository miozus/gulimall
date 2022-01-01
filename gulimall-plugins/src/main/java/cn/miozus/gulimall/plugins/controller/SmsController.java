package cn.miozus.gulimall.plugins.controller;

import cn.miozus.common.utils.R;
import cn.miozus.gulimall.plugins.component.SmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 发送短信控制器，供给其他服务使用
 *
 * @author miao
 * @date 2021/12/28
 */
@RestController
@RequestMapping("/sms")
public class SmsController {

    @Autowired
    SmsComponent smsComponent;


    @GetMapping("/sendcode")
    public R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code) {
        smsComponent.sendSMS(phone, code);
        return R.ok();
    }

}
