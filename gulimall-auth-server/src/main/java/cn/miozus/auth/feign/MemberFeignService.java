package cn.miozus.auth.feign;

import cn.miozus.auth.vo.SocialUser;
import cn.miozus.auth.vo.UserLoginVo;
import cn.miozus.auth.vo.UserRegisterVo;
import cn.miozus.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 远程调用会员服务
 *
 * @author miao
 * @date 2021/12/30
 */
@FeignClient("gulimall-member")
public interface MemberFeignService {

    /**
     * 注册
     *
     * @param vo vo
     * @return {@link R}
     * @see R
     */
    @PostMapping("/member/member/register")
    R register(@RequestBody UserRegisterVo vo);

    /**
     * 检查登录
     *
     * @param vo 签证官, 前端传来KV，远程传递 JSON
     * @return {@link R}
     */
    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);

    /**
     * oauth登录
     *
     * @param socialUser 社会用户
     * @return {@link R}
     */
    @PostMapping("/member/member/oauth2/login")
    R oauthLogin(@RequestBody SocialUser socialUser);

}
