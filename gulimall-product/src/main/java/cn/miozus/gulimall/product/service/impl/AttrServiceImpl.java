package cn.miozus.gulimall.product.service.impl;

import cn.miozus.common.constant.ProductConstant;
import cn.miozus.gulimall.product.dao.AttrAttrgroupRelationDao;
import cn.miozus.gulimall.product.dao.AttrGroupDao;
import cn.miozus.gulimall.product.dao.CategoryDao;
import cn.miozus.gulimall.product.entity.AttrAttrgroupRelationEntity;
import cn.miozus.gulimall.product.entity.AttrGroupEntity;
import cn.miozus.gulimall.product.entity.CategoryEntity;
import cn.miozus.gulimall.product.service.CategoryService;
import cn.miozus.gulimall.product.vo.AttrRespVo;
import cn.miozus.gulimall.product.vo.AttrVo;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;

import cn.miozus.gulimall.product.dao.AttrDao;
import cn.miozus.gulimall.product.entity.AttrEntity;
import cn.miozus.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    AttrAttrgroupRelationDao relationDao;

    @Autowired
    AttrGroupDao attrGroupDao;


    @Autowired
    CategoryDao categoryDao;

    @Autowired
    AttrService attrService;

    @Autowired
    CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        // 1.保存自身基本数据
        AttrEntity attrEntity = new AttrEntity();
        // 单个操作麻烦，用工具类批量复制 属性类一一对应
        BeanUtils.copyProperties(attr, attrEntity);
        this.save(attrEntity);
        // 2.保存关联关系（排除基本属性）
        if (attr.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()
                && attr.getAttrGroupId() != null) {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attrEntity.getAttrId());
            relationDao.insert(relationEntity);
        }
    }

    /**
     * 查询attr页面
     *
     * @param params   分页参数
     * @param catlogId 目录id
     * @param attrType 枚举类：base / sale
     * @return {@link PageUtils}
     */
    @Override
    public PageUtils queryAttrPage(Map<String, Object> params, Long catlogId, String attrType) {
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<>();
        // 不选择菜单时，且菜单有分类
        if (catlogId != 0) {
            wrapper.eq("catelog_id", catlogId);
        }
        // 模糊查询
        String key = (String) params.get("key");
        if (StringUtils.isNotEmpty(key)) {
            wrapper.and(condition ->
                    condition.eq("attr_id", key).or().like("attr_name", key)
            );
        }
        // 分页数据，渲染复杂条件; 销售属性只显示 0 ; 规格参数 base 显示所有；
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params), wrapper.eq("attr_type",
                        "base".equalsIgnoreCase(attrType) ?
                                ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() :
                                ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode()
                ));
        // 分页插件，查询结果上继续从 DO 层分别查询数据
        PageUtils pageUtils = new PageUtils(page);
        if ("base".equalsIgnoreCase(attrType)) {
            List<AttrEntity> records = page.getRecords();
            List<AttrRespVo> respVos = records.stream().map(attrEntity -> {
                AttrRespVo attrRespVo = new AttrRespVo();
                BeanUtils.copyProperties(attrEntity, attrRespVo);
                // 设置分组的名字：属性表 > 关系表 > 属性分组表 > 名字
                AttrAttrgroupRelationEntity relationEntity = relationDao.selectOne(
                        // 复杂条件查询，返回一条数据集
                        new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId())
                );
                if (relationEntity != null && relationEntity.getAttrGroupId() != null) {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(relationEntity.getAttrGroupId());
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
                // 设置分类的名字：属性表 > 目录表 > 名字
                CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
                if (categoryEntity != null) {
                    attrRespVo.setCatelogName(categoryEntity.getName());
                }
                return attrRespVo;
            }).collect(Collectors.toList());
            pageUtils.setList(respVos);

        }
        return pageUtils;
    }

    @Override
    public AttrRespVo getAttrById(Long attrId) {
        // 查询基本信息
        AttrEntity attrEntity = this.baseMapper.selectById(attrId);
        AttrRespVo attrRespVo = new AttrRespVo();
        BeanUtils.copyProperties(attrEntity, attrRespVo);

        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            // 设置分组名字：属性表 > 关系表 > 属性分组表 > 名字
            AttrAttrgroupRelationEntity relationEntity = relationDao.selectOne(
                    // 复杂条件查询，返回一条数据集
                    new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId())
            );
            if (relationEntity != null) {
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(relationEntity.getAttrGroupId());
                if (attrGroupEntity != null) {
                    // 原生字段未有，需要重新添加一遍，前端才能显示
                    attrRespVo.setAttrGroupId(relationEntity.getAttrGroupId());
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }
        // 设置分类名字：属性表 > 目录表 > 名字
        CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
        if (categoryEntity != null) {
            attrRespVo.setCatelogName(categoryEntity.getName());
        }
        // 设置分组路径：目录表服务 > 路径
        Long[] catelogPath = categoryService.findCatelogPath(attrEntity.getCatelogId());
        if (catelogPath != null) {
            attrRespVo.setCatelogPath(catelogPath);
        }

        return attrRespVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        // 保存自身数据
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.updateById(attrEntity);
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            // 保存额外两个字段（分组关联）
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attr.getAttrId());
            Integer count = relationDao.selectCount(new UpdateWrapper<AttrAttrgroupRelationEntity>()
                    .eq("attr_id", attrEntity.getAttrId()));
            if (count > 0) {
                relationDao.update(relationEntity, new UpdateWrapper<AttrAttrgroupRelationEntity>()
                        .eq("attr_id", attrEntity.getAttrId()));
            } else {
                relationDao.insert(relationEntity);
            }
        }

    }

    /**
     * 根据分组 id 查找关联的所有属性；
     * 关联表： 分组id ~ 属性id >  属性表：所有
     * 这是一组一组地查，所以用 selectList
     * 页面只需要属性名和可选值，所以基本的 Entity 足够
     *
     * @param attrgroupId attrgroup id
     * @return {@link List<AttrEntity>}
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        List<AttrAttrgroupRelationEntity> relationEntities = relationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupId));
        List<Long> attrIds = relationEntities.stream()
                .map(AttrAttrgroupRelationEntity::getAttrId)
                .collect(Collectors.toList());

        if (attrIds.isEmpty()) {
            return null;
        }

        return this.listByIds(attrIds);
    }

    /**
     * 获取当前分组，没有关联的所有属性
     *
     * @param params      参数个数
     * @param attrgroupId attrgroup id
     * @return {@link PageUtils}
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        // 当前分组，只能关联自己所属的分类 的所有属性， 所以从 attrgroupId 为主键的的表
        // 当前分组，只能关联别的分组，没有引用的属性（一个属性只能与一个属性分组绑定）（属性：分组~1：1）
        // attrGroupId:catlogId ~ 1:N  所以从一个 catlogId 找到它的同类
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();
        // 1- 属性分组表：当前分类下的其他分组 1-> N1 💧  N2 <- N1 如同水滴溅射
        List<Long> attrGroupIds = attrGroupDao.selectList(
                new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId)
        ).stream().map(AttrGroupEntity::getAttrGroupId).collect(Collectors.toList());
        // 2- 关联表：中转翻译，为查询其关联的属性做准备
        List<Long> attrIds = relationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", attrGroupIds)
        ).stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());
        // 3- 属性表：当前分类的所有属性 - 移除（notIn）这些关联的属性
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>()
                .eq("catelog_id", catelogId)
                .eq("search_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        if (CollectionUtils.isNotEmpty(attrIds)) {
            wrapper.notIn("attr_id", attrIds);
        }
        // 模糊查询
        String key = (String) params.get("key");
        if (StringUtils.isNotEmpty(key)) {
            wrapper.and(w -> w.eq("attr_id", key).or().like("attr_name", key));
        }
        // 汇总
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);

        return new PageUtils(page);
    }
}