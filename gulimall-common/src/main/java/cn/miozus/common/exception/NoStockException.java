package cn.miozus.common.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * 零库存异常
 *
 * @author miao
 * @date 2022/01/22
 */
public class NoStockException extends RuntimeException {
    @Getter
    @Setter
    private Long skuId;

    public NoStockException(Long skuId) {
        super("skuId: " + skuId + " 库存不足");
    }

    public NoStockException(String msg) {
        super("msg: " + msg + "库存不足");
    }

}
