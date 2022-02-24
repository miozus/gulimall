package cn.miozus.gulimall.auth.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * 用户登录签证官
 *
 * @author miao
 * @date 2021/12/30
 */
@Data
public class UserLoginVo {

    @NotBlank(message = "用户名不能为空")
    @Length(min = 6, max = 18, message = "用户名必须是6-18位字符")
    private String account;

    @Length(min = 6, max = 18, message = "密码必须是6-18位字符")
    @NotBlank(message = "密码不能为空")
    private String password;
}
