package cn.miozus.auth.feign;

import cn.miozus.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 插件服务
 *
 * @author miao
 * @date 2021/12/28
 */
@FeignClient("gulimall-plugins")
public interface PluginsFeignService {

    /**
     * 发送短信验证码
     *
     * @param phone 电话
     * @param code  验证码
     * @return {@link R}
     * @see R
     */
    @GetMapping("/sms/sendcode")
    R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code);


}
