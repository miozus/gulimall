package cn.miozus.common.to.es;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * sku es模型
 *
 * @author miao
 * @date 2021/10/01
 */
@Data
public class SkuEsModel {

    private Long skuId;
    private Long spuId;
    private String skuTitle;
    /** sku价格 保持精度 */
    private BigDecimal skuPrice;
    private String skuImg  ;

    private Long saleCount;
    private Boolean hasStock;
    private Long hotScore;
    private Long brandId;
    private Long catalogId;
    private String brandName;
    private String brandImg;
    private String catalogName;
    private List<Attrs> attrs;

    /**
     * sku es attrs
     * public 可访问权限，则可序列化
     *
     * @author miao
     * @date 2021/10/01
     */
    @Data
    public static class Attrs {
        private Long attrId;
        private String attrName;
        private String attrValue;
    }


}