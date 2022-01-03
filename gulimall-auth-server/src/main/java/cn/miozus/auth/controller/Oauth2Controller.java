package cn.miozus.auth.controller;

import cn.miozus.auth.api.GiteeApi;
import cn.miozus.auth.feign.MemberFeignService;
import cn.miozus.auth.vo.MemberRespVo;
import cn.miozus.auth.vo.SocialUser;
import cn.miozus.common.utils.R;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * 社交登录：换取令牌和获取用户公开信息
 *
 * @author miao
 * @date 2022/01/02
 */
@Controller
@Slf4j
public class Oauth2Controller {

    @Autowired
    GiteeApi giteeApi;
    @Autowired
    MemberFeignService memberFeignService;

    @GetMapping("/oauth2/gitee/redirect")
    public String gitee(@RequestParam("code") String code, RedirectAttributes redirectAttributes){
        SocialUser socialUser = giteeApi.fetchToken(code);
        R r = memberFeignService.oauthLogin(socialUser);
        if (r.getCode() != 0) {
            Map<String, String> errors = new HashMap<>(1);
            errors.put("msg", r.getData("msg", new TypeReference<String>() {
            }));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
        MemberRespVo memberRespVo = r.getData("data", new TypeReference<MemberRespVo>() {
        });
        String username = memberRespVo.getUsername();
        Map<String, String> userInfo = new HashMap<>(1);
        userInfo.put("username", username);
        redirectAttributes.addFlashAttribute("userInfo", userInfo);
        return "redirect:http://gulimall.com";
    }
}
