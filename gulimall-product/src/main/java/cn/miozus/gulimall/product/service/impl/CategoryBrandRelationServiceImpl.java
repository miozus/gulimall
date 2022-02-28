package cn.miozus.gulimall.product.service.impl;

import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.common.utils.Query;
import cn.miozus.gulimall.product.dao.BrandDao;
import cn.miozus.gulimall.product.dao.CategoryBrandRelationDao;
import cn.miozus.gulimall.product.dao.CategoryDao;
import cn.miozus.gulimall.product.entity.BrandEntity;
import cn.miozus.gulimall.product.entity.CategoryBrandRelationEntity;
import cn.miozus.gulimall.product.entity.CategoryEntity;
import cn.miozus.gulimall.product.service.BrandService;
import cn.miozus.gulimall.product.service.CategoryBrandRelationService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    @Autowired
    BrandDao brandDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationDao relationDao;

    @Autowired
    BrandService brandService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存细节：中间表冗余存储两个名字;（每张表都要更新细节）
     *
     * @param categoryBrandRelation 类别品牌关系
     */
    @Override
    public void saveDetails(CategoryBrandRelationEntity categoryBrandRelation) {
        Long brandId = categoryBrandRelation.getBrandId();
        Long catalogId = categoryBrandRelation.getCatalogId();
        // 查询详细名字和分类名，需要这两个 DO
        BrandEntity brandEntity = brandDao.selectById(brandId);
        CategoryEntity categoryEntity = categoryDao.selectById(catalogId);
        // 设置关联关系
        categoryBrandRelation.setBrandName(brandEntity.getName());
        categoryBrandRelation.setCatalogName(categoryEntity.getName());
        // 调用保存, 自己的 DO ( baseMapper / service)
        this.save(categoryBrandRelation);
    }

    @Override
    public void updateBrand(Long brandId, String name) {
        CategoryBrandRelationEntity relationEntity = new CategoryBrandRelationEntity();
        relationEntity.setBrandId(brandId);
        relationEntity.setBrandName(name);

        // 按照索引id，更新字段
        this.update(relationEntity, new UpdateWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId));
    }

    @Override
    public void updateCategory(Long catId, String name) {
        this.baseMapper.updateCategory(catId, name);
    }

    @Override
    public List<BrandEntity> getBrandListByCatId(Long catId) {
        List<CategoryBrandRelationEntity> entites = relationDao
                .selectList(new QueryWrapper<CategoryBrandRelationEntity>().eq("catalog_id", catId));
        // 注入服务更好，顺便拓展丰富常用方法，而Dao都是自动生成的，将就用；
        return entites.stream()
                .map(entity -> brandService.getById(entity.getBrandId()))
                .collect(Collectors.toList());
    }

}