package cn.miozus.gulimall.product.service.impl;

import cn.miozus.common.to.SkuReductionTo;
import cn.miozus.common.to.SpuBoundTo;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.common.utils.R;
import cn.miozus.gulimall.product.dao.SpuInfoDao;
import cn.miozus.gulimall.product.entity.*;
import cn.miozus.gulimall.product.feign.CouponFeignService;
import cn.miozus.gulimall.product.service.*;
import cn.miozus.gulimall.product.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    SpuImagesService spuImagesService;

    @Autowired
    ProductAttrValueService partAttrValueService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    CouponFeignService couponFeignService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * spu保存信息
     * TODO: 高级部分完善网络卡顿、部分数据回滚、异常集中收集的情况
     * @param vo 签证官
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        // 保存 spu 基本信息 pms_spu_info
        SpuInfoEntity infoEntity = new SpuInfoEntity(); // 相当于注入新的记录，生成新的自增id
        BeanUtils.copyProperties(vo, infoEntity);
        infoEntity.setCreateTime(new Date()); // 数据库中和实体类多了创建和更新时间记录，需要现在设置
        infoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(infoEntity);
        // 保存 spu 描述(长图文) pms_spu_info_desc
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        Long spuId = infoEntity.getId();
        descEntity.setSpuId(spuId);  // 因为大数字，设定非自增，需要查询（自增id）获取
        descEntity.setDecript(String.join(",", decript)); // 可迭代的集合，将list分割字符串
        spuInfoDescService.saveSpuInfoDesc(descEntity);
        // 保存 spu 图片集 pms_spu_images
        List<String> images = vo.getImages();
        spuImagesService.saveSpuImages(spuId, images); // ?! 名称、分类、默认图被忽略了
        // 保存 spu 规格参数 pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        partAttrValueService.saveProductAttrs(spuId, baseAttrs);

        // 保存 spu 积分 gulimall_sms: sms_spu_bounds  // 跨表最后再写，先写简单的
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        Bounds bounds = vo.getBounds();
        BeanUtils.copyProperties(bounds, spuBoundTo);
        spuBoundTo.setSpuId(spuId);
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if (r.getCode() != 0) {
            log.error("远程保存 spu 积分信息失败");
        }

        // 保存库存信息：基本信息 pms_sku_info  // 展开遍历，方便接下来的步骤复用
        List<Skus> skus = vo.getSkus();
        if (CollectionUtils.isNotEmpty(skus)) {
            skus.forEach(sku -> {
                String defaultImg = "";  // 匿名函数不支持赋值给string类型，所以写成遍历
                for (Images image : sku.getImages()) {
                    if (image.getDefaultImg() == 1) {
                        defaultImg = image.getImgUrl();  // 字符类型
                    }
                }
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(sku, skuInfoEntity);
                skuInfoEntity.setBrandId(infoEntity.getBrandId());
                skuInfoEntity.setCatalogId(infoEntity.getCatalogId());
                skuInfoEntity.setSpuId(spuId);
                //skuInfoEntity.setSkuDesc(sku.getDescar()); // "descar": ["翡翠冷", "8GB+256GB"],
                skuInfoEntity.setSaleCount(0L); // 初始默认值销量为零
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                skuInfoService.saveSkuInfo(skuInfoEntity);

                Long skuId = skuInfoEntity.getSkuId(); // skuId 保存完毕后生成自增,供以下复用

                // 保存库存信息：图片 pms_sku_images
                // TODO：没有图片路径的，无需保存（未选中的）
                List<SkuImagesEntity> imagesEntities = sku.getImages().stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(image.getImgUrl());
                    skuImagesEntity.setDefaultImg(image.getDefaultImg());  // 整型类型
                    return skuImagesEntity;
                }).filter(entity -> StringUtils.isNotBlank(entity.getImgUrl()) // 返回 true 表示需要，则保留下来
                ).collect(Collectors.toList());
                skuImagesService.saveBatch(imagesEntities);

                // 保存库存信息：销售属性 pms_sku_sale_attr_value
                List<Attr> skuAttrs = sku.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = skuAttrs.stream().map(attr -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(attr, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    // TODO: attrSort default 0 ?
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

                // 保存库存信息：优惠满减（跨库） gulimall_sms:
                // sms_sku_ladder（打折）,sms_sku_full_reduction（满减）,sms_member_price（会员价）
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(sku, skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                if (skuReductionTo.getFullCount() > 0
                        || skuReductionTo.getFullPrice().compareTo(BigDecimal.ZERO) > 0) {
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (r1.getCode() != 0) {
                        log.error("远程保存 sku 积分信息失败");
                    }
                }
            });
        }


    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }


}