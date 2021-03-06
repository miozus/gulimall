package cn.miozus.gulimall.product.service;

import cn.miozus.gulimall.product.vo.AttrGroupRelationVo;
import cn.miozus.gulimall.product.vo.AttrGroupWithAttrVo;
import cn.miozus.gulimall.product.vo.SkuItemVo;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.product.entity.AttrGroupEntity;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-06 23:57:18
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPage(Map<String, Object> params, Long catalogId);

    void removeRelation(AttrGroupRelationVo[] params);

    List<AttrGroupWithAttrVo> getAttrGroupWithAttrsByCatalogId(Long catalogId);

    List<SkuItemVo.SpuItemGroupAttrVo> queryAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId);
}

