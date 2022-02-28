package cn.miozus.gulimall.product.service.impl;

import cn.miozus.gulimall.product.service.CategoryBrandRelationService;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.common.utils.Query;

import cn.miozus.gulimall.product.dao.BrandDao;
import cn.miozus.gulimall.product.entity.BrandEntity;
import cn.miozus.gulimall.product.service.BrandService;


@Service("brandService")
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        // 获取key
        QueryWrapper<BrandEntity> queryWrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.eq("brand_id", key).or().like("name", key);
        }
        IPage<BrandEntity> page = this.page(
                new Query<BrandEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void updateDetails(BrandEntity brand) {
        // 保证冗余字段数据一致
        this.updateById(brand);
        if (StringUtils.isNotEmpty(brand.getName())){
            // 同步其他关联表中数据
           categoryBrandRelationService.updateBrand(brand.getBrandId(),brand.getName());

           // TODO: 更新其他关联

        }
    }

}