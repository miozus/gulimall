package cn.miozus.gulimall.product.service.impl;

import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.gulimall.product.dao.ProductAttrValueDao;
import cn.miozus.gulimall.product.entity.AttrEntity;
import cn.miozus.gulimall.product.entity.ProductAttrValueEntity;
import cn.miozus.gulimall.product.service.AttrService;
import cn.miozus.gulimall.product.service.ProductAttrValueService;
import cn.miozus.gulimall.product.vo.BaseAttrs;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    @Autowired
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<ProductAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveProductAttrs(Long id, List<BaseAttrs> baseAttrs) {
        if (CollectionUtils.isNotEmpty(baseAttrs)) {
            List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
                ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();

                valueEntity.setSpuId(id);
                valueEntity.setAttrId(attr.getAttrId());
                AttrEntity attrEntity = attrService.getById(attr.getAttrId());
                valueEntity.setAttrName(attrEntity.getAttrName());  // 先留空；再调用表查询时补上；
                valueEntity.setAttrValue(attr.getAttrValues());
                valueEntity.setQuickShow(attr.getShowDesc());
                return valueEntity;

            }).collect(Collectors.toList());
            this.saveBatch(collect);
        }}

}