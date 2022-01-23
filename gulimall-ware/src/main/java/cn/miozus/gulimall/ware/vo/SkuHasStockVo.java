package cn.miozus.gulimall.ware.vo;

import lombok.Data;

/**
 * sku商品有库存
 *
 * @author miao
 * @date 2021/10/01
 */
@Data
public class SkuHasStockVo {
    private Long skuId;
    private Boolean hasStock;
}
