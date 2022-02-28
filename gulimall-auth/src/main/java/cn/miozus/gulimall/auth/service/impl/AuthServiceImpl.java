package cn.miozus.gulimall.auth.service.impl;

import cn.miozus.gulimall.common.annotation.Idempotent;
import cn.miozus.gulimall.common.constant.AuthServerConstant;
import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.auth.feign.MemberFeignService;
import cn.miozus.gulimall.auth.feign.PluginsFeignService;
import cn.miozus.gulimall.auth.service.AuthService;
import cn.miozus.gulimall.auth.vo.UserLoginVo;
import cn.miozus.gulimall.auth.vo.UserRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 身份验证服务impl
 *
 * @author miozus
 * @date 2022/02/22
 */
@Service("authService")
public class AuthServiceImpl implements AuthService {

    @Autowired
    MemberFeignService memberFeignService;
    @Autowired
    PluginsFeignService pluginsFeignService;

    @Override
    @Idempotent(value = "短信验证码防刷", time = AuthServerConstant.CODE_TOKEN_TIME)
    public R sendCode(String phone, String code) {
//         pluginsFeignService.sendCode(phone, code);
        return R.ok();
    }

    @Override
    @Idempotent("短信验证码校验")
    public R register(UserRegisterVo vo) {
        return memberFeignService.register(vo);
    }

    @Override
    public R login(UserLoginVo vo) {
        return memberFeignService.login(vo);
    }
}
