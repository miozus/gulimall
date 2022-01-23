package cn.miozus.gulimall.order.vo;

import lombok.Data;

/**
 * 每件商品锁定结果回执
 *
 * @author miao
 * @date 2022/01/22
 */
@Data
public class LockStockResult {
    private Long skuId;
    private Integer count;
    private boolean locked;
}
