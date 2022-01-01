package cn.miozus.auth.vo;

import lombok.Data;

/**
 * 用户登录签证官
 *
 * @author miao
 * @date 2021/12/30
 */
@Data
public class UserLoginVo {

    private String account;
    private String password;
}
