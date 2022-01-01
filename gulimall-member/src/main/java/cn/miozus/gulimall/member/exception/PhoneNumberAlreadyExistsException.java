package cn.miozus.gulimall.member.exception;

/**
 * 手机存在异常
 *
 * @author miao
 * @date 2021/12/30
 */
public class PhoneNumberAlreadyExistsException extends RuntimeException {
    public PhoneNumberAlreadyExistsException() {
        super("手机号已存在");
    }
}
