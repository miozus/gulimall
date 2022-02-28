package cn.miozus.gulimall.auth.service;

import cn.miozus.gulimall.common.exception.GuliMallBindException;
import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.auth.vo.UserLoginVo;
import cn.miozus.gulimall.auth.vo.UserRegisterVo;

/**
 * 身份验证服务
 *
 * @author miozus
 * @date 2022/02/22
 */
public interface AuthService {
    /**
     * 发送短信验证码
     *
     * @param phone 电话
     * @param code  缺省值，在Redis切面中生成自动修改执行结果
     * @return {@link R}
     */
    R sendCode(String phone, String code) throws GuliMallBindException;

    /**
     * 注册
     *
     * @param vo 视图对象
     * @return {@link R}
     */
    R register(UserRegisterVo vo);

    /**
     * 登录
     *
     * @param vo 视图对象
     * @return {@link R}
     */
    R login(UserLoginVo vo);
}
