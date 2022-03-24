package cn.miozus.gulimall.product.vo;

import cn.miozus.gulimall.product.entity.SkuImagesEntity;
import cn.miozus.gulimall.product.entity.SkuInfoEntity;
import cn.miozus.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

/**
 * sku 商品详情页视图模型
 * <p>
1. sku 基本信息
2. sku 图片
3. sku 销售属性
4. spu 商品介绍: 所有的描述（一张长图）
5. sku 规格与包装：(分组 > 商品规格属性键值对)*N
 *
 * @author miao
 * @date 2021/12/24
 */
@Data
public class SkuItemVo {
    SkuInfoEntity info;
    Boolean hasStock = true;
    List<SkuImagesEntity> images;
    List<SkuItemSaleAttrVo> saleAttr;
    SpuInfoDescEntity desc;
    List<SpuItemGroupAttrVo> groupAttrs;
    /** 附加秒杀信息 */
    SeckillSkuRedisTo seckillInfo;

    /** sku 商品销售属性视图对象 */
    @Data
    public static class SkuItemSaleAttrVo {
        private Long attrId;
        private String attrName;
        private List<AttrValueWithSkuIdVo> attrValues;

    }

    /** 每个销售属性在哪些 sku 中出现过，方便求交集 */
    @Data
    public static class AttrValueWithSkuIdVo {
        private String attrValue;
        private String skuIds;
    }

    /** 规格与包装：分组 > 属性键值对 */
    @Data
    public static class SpuItemGroupAttrVo {
        private String groupName;
        private List<SpuBaseAttrVo> attrs;
    }

    /** spu 基本属性键值对 */
    @Data
    public static class SpuBaseAttrVo {
        private String attrName;
        private String attrValue;
    }

}
