package cn.miozus.gulimall.member.exception;

public class UsernameNotExistsException extends RuntimeException {

    public UsernameNotExistsException() {
        super("用户名不存在");
    }
}

