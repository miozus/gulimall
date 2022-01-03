package cn.miozus.gulimall.member.vo;

import lombok.Data;

/**
 * 社交用户
 *
 * @author miao
 * @date 2022/01/02
 */
@Data
public class SocialUser {
    private Long socialUid;
    private String expiresIn;
    private String accessToken;
}
