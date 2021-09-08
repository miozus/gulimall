package cn.miozus.gulimall.product.service.impl;

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
        // 保存自身基本数据
        AttrEntity attrEntity = new AttrEntity();
        // 单个操作麻烦，用工具类批量复制 属性类一一对应
        BeanUtils.copyProperties(attr, attrEntity);
        this.save(attrEntity);
        // 保存关联关系
        AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
        relationEntity.setAttrGroupId(attr.getAttrGroupId());
        relationEntity.setAttrId(attrEntity.getAttrId());
        relationDao.insert(relationEntity);
    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catlogId) {
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
        // 分页数据，渲染复杂条件
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                wrapper
        );
        // 分页插件，查询结果上继续从 DO 层分别查询数据
        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();
        List<AttrRespVo> respVos = records.stream().map(attrEntity -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);
            // 设置分组的名字：属性表 > 关系表 > 属性分组表 > 名字
            AttrAttrgroupRelationEntity relationEntity = relationDao.selectOne(
                    // 复杂条件查询，返回一条数据集
                    new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId())
            );
            if (relationEntity != null) {
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

        return pageUtils;
    }

    @Override
    public AttrRespVo getAttrById(Long attrId) {
        // 查询基本信息
        AttrEntity attrEntity = this.baseMapper.selectById(attrId);
        AttrRespVo attrRespVo = new AttrRespVo();
        BeanUtils.copyProperties(attrEntity, attrRespVo);
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
        // 求同存异
        BeanUtils.copyProperties(attr, attrEntity);
        this.updateById(attrEntity);
        // 保存额外两个字段（分组关联）
        AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
        relationEntity.setAttrGroupId(attr.getAttrGroupId());
        relationEntity.setAttrId(attr.getAttrId());
        Integer count = relationDao.selectCount(new UpdateWrapper<AttrAttrgroupRelationEntity>()
                .eq("attr_id", attrEntity.getAttrId()));
        if (count > 0){
            relationDao.update(relationEntity, new UpdateWrapper<AttrAttrgroupRelationEntity>()
                    .eq("attr_id", attrEntity.getAttrId()));
        } else {
            relationDao.insert(relationEntity);
        }

    }


}