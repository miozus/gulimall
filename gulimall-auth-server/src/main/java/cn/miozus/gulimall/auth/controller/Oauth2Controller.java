package cn.miozus.gulimall.auth.controller;

import cn.miozus.gulimall.auth.api.GiteeApi;
import cn.miozus.gulimall.auth.feign.MemberFeignService;
import cn.miozus.common.constant.AuthServerConstant;
import cn.miozus.common.vo.MemberRespVo;
import cn.miozus.gulimall.auth.vo.SocialUser;
import cn.miozus.common.utils.R;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
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
    public String gitee(@RequestParam("code") String code, RedirectAttributes redirectAttributes, HttpSession session) {
        SocialUser socialUser = giteeApi.fetchToken(code);
        R r = null;
        try {
            r = memberFeignService.oauthLogin(socialUser);
            if (!r.isEmpty()) {
                MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
                });
                session.setAttribute(AuthServerConstant.LOGIN_USER, data);
                log.info("登陆成功 data {}", data);
                return "redirect:http://gulimall.com";
            }
        } catch (Exception e) {
            r = R.error("远程调用失败：可能网络延迟高");
            e.printStackTrace();
        }
        Map<String, String> errors = new HashMap<>(1);
        errors.put("msg", r.getData("msg", new TypeReference<String>() {
        }));
        redirectAttributes.addFlashAttribute("errors", errors);
        return "redirect:http://auth.gulimall.com/login.html";
    }

}
