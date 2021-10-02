package cn.miozus.common.to;

import lombok.Data;

/**
 * sku有股票签证官
 *
 * @author miao
 * @date 2021/10/01
 */
@Data
public class SkuHasStockVo {
    private Long skuId;
    private Boolean hasStock;
}
