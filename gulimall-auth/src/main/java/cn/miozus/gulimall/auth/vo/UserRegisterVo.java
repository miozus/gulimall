package cn.miozus.gulimall.auth.vo;

import cn.miozus.common.constant.RegexConstant;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

/**
 * 用户注册签证官
 *
 * @author miao
 * @date 2021/12/29
 */
@Data
public class UserRegisterVo {

    @NotEmpty(message = "用户名必须填写")
    @Length(min = 6, max = 18, message = "用户名必须是6-18位字符")
    private String username;

    @Length(min = 6, max = 18, message = "密码必须是6-18位字符")
    @NotEmpty(message = "密码必须填写")
    private String password;

    @Pattern(regexp = RegexConstant.CHINA_MOBILE, message = "手机号格式不正确")
    @NotEmpty(message = "手机号必须填写")
    private String phone;

    @NotEmpty(message = "验证码必须填写")
    private String code;
}
