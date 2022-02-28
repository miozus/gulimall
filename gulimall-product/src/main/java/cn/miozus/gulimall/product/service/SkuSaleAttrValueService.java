package cn.miozus.gulimall.product.service;

import cn.miozus.gulimall.product.vo.SkuItemVo;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.product.entity.SkuSaleAttrValueEntity;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-06 23:57:18
 */
public interface SkuSaleAttrValueService extends IService<SkuSaleAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 商品详情页，销售属性的组合
     *
     * @param spuId spu id
     * @return {@link List}<{@link SkuItemVo.SkuItemSaleAttrVo}>
     */
    List<SkuItemVo.SkuItemSaleAttrVo> querySaleAttrsBySpuId(Long spuId);

    /**
     * 查询sku 销售属性的字符串，用于渲染购物车商品信息
     *
     * @param skuId sku id
     * @return {@link List}<{@link String}>
     */
    List<String> querySkuAttrs(Long skuId);
}

