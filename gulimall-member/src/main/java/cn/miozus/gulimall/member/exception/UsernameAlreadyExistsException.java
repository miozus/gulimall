package cn.miozus.gulimall.member.exception;

/**
 * 用户名存在异常
 *
 * @author miao
 * @date 2021/12/30
 */
public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException() {
        super("用户名已存在");
    }
}
