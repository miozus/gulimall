package cn.miozus.gulimall.cart.to;

import lombok.Data;

/**
 * 用户信息签证官
 *
 * @author miao
 * @date 2022/01/04
 */
@Data
public class UserInfoTo {
    /**
     * 已登录用户：在数据库中表的第 N 行
     */
    private Long userId;
    /**
     * 临时用户凭证
     */
    private String userKey;
    /**
     * 临时用户: Cookie 中是否放入，在 postHandle 中储存，相当于读写锁
     */
    private boolean hasTempUser = false;
}
