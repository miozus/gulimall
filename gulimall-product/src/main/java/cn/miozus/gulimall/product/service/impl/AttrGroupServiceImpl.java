package cn.miozus.gulimall.product.service.impl;

import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.common.utils.Query;
import cn.miozus.gulimall.product.dao.AttrAttrgroupRelationDao;
import cn.miozus.gulimall.product.dao.AttrDao;
import cn.miozus.gulimall.product.dao.AttrGroupDao;
import cn.miozus.gulimall.product.entity.AttrAttrgroupRelationEntity;
import cn.miozus.gulimall.product.entity.AttrEntity;
import cn.miozus.gulimall.product.entity.AttrGroupEntity;
import cn.miozus.gulimall.product.service.AttrGroupService;
import cn.miozus.gulimall.product.service.AttrService;
import cn.miozus.gulimall.product.vo.AttrGroupRelationVo;
import cn.miozus.gulimall.product.vo.AttrGroupWithAttrVo;
import cn.miozus.gulimall.product.vo.SkuItemVo;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    AttrAttrgroupRelationDao relationDao;

    @Autowired
    AttrGroupService attrGroupService;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    AttrService attrService;

    @Autowired
    AttrDao attrDao;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catalogId) {
        /** SQL 模糊查询
         select * from pms_attr_group
         where catalog_id=?
         and (attr_group_id=key
         or attr_group_name like %key%)
         */
        String key = (String) params.get("key");
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(key)) {
            wrapper.and(obj ->
                    obj.eq("attr_group_id", key)
                            .or()
                            .like("attr_group_name", key));
        }
        if (catalogId == 0) {
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    wrapper
            );
            return new PageUtils(page);
        } else {
            wrapper.eq("catalog_id", catalogId);
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    wrapper
            );
            return new PageUtils(page);
        }

    }

    @Override
    public void removeRelation(AttrGroupRelationVo[] vos) {
        List<AttrAttrgroupRelationEntity> entities = Arrays.stream(vos).map(vo -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(vo, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());

        relationDao.deleteBatchRelation(entities);

    }

    /**
     * 被catlog attr组attrs id
     *
     * @param catalogId catalog id
     * @return {@link List<AttrGroupWithAttrVo>}
     */
    @Override
    public List<AttrGroupWithAttrVo> getAttrGroupWithAttrsByCatalogId(Long catalogId) {

        // 查询属性分组
        List<AttrGroupEntity> attrGroupEntities = this.list(
                new QueryWrapper<AttrGroupEntity>().eq("catalog_id", catalogId)
        );
        // 查询属性分组关联的所有属性（已写过方法）
        if (CollectionUtils.isNotEmpty(attrGroupEntities)) {
            return attrGroupEntities.stream().map(entity -> {
                AttrGroupWithAttrVo attrGroupWithAttrVo = new AttrGroupWithAttrVo();
                BeanUtils.copyProperties(entity, attrGroupWithAttrVo);
                List<AttrEntity> attrs = attrService.getRelationAttr(attrGroupWithAttrVo.getAttrGroupId());
                attrGroupWithAttrVo.setAttrs(attrs);
                return attrGroupWithAttrVo;
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 多表联查：查询出当前 spu 对应的所有属性所有分组信息，以及分组下所有属性对应的值
     *
     * @return {@link List}
     * @see List
     */
    @Override
        public List<SkuItemVo.SpuItemGroupAttrVo> queryAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {
        AttrGroupDao baseMapper = this.getBaseMapper();
        return baseMapper.getAttrGroupWithAttrsBySpuId(spuId, catalogId);
    }
}
