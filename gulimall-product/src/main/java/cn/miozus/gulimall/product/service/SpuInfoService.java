package cn.miozus.gulimall.product.service;

import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.product.entity.SpuInfoEntity;
import cn.miozus.gulimall.product.vo.SpuSaveVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * spu信息
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-06 23:57:18
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuSaveVo spuInfo);

    void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity);

    PageUtils queryPageByCondition(Map<String, Object> params);

    void publish(Long spuId);

    /**
     * 查询spu信息按sku id
     *
     * @param skuId sku id
     * @return {@link SpuInfoEntity}
     */
    SpuInfoEntity querySpuInfoBySkuId(Long skuId);
}

