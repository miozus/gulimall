package cn.miozus.common.exception;

/**
 * OpenFeign 调用异常
 *
 * @author miao
 * @date 2022/01/25
 */
public class FeignDeliverException extends RuntimeException {

    public FeignDeliverException() {
        super("远程调用订单服务失败");
    }

}
