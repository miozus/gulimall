package cn.miozus.gulimall.product.service.impl;

import cn.miozus.common.constant.ProductConstant;
import cn.miozus.common.to.SkuHasStockVo;
import cn.miozus.common.to.SkuReductionTo;
import cn.miozus.common.to.SpuBoundTo;
import cn.miozus.common.to.es.SkuEsModel;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.common.utils.R;
import cn.miozus.gulimall.product.dao.SpuInfoDao;
import cn.miozus.gulimall.product.entity.*;
import cn.miozus.gulimall.product.feign.CouponFeignService;
import cn.miozus.gulimall.product.feign.SearchFeignService;
import cn.miozus.gulimall.product.feign.WareFeignService;
import cn.miozus.gulimall.product.service.*;
import cn.miozus.gulimall.product.vo.*;
import com.alibaba.fastjson.TypeReference;
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
import java.util.*;
import java.util.stream.Collectors;


/**
 * spu impl信息服务
 *
 * @author miao
 * @date 2021/10/01
 */
@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    SpuImagesService spuImagesService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    BrandService brandService;

    @Autowired
    AttrService attrService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    SearchFeignService searchFeignService;


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
     *
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
        productAttrValueService.saveProductAttrs(spuId, baseAttrs);
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

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        /*
            status:
            key: 大米
            brandId: 0
            catelogId: 0
         */
        String key = (String) params.get("key");
        if (StringUtils.isNotBlank(key)) {
            // and ( ... ) 拼接的条件放在括号内，否则类似 or 1=1 永真
            wrapper.and(w ->
                    w.eq("id", key)
                            .or()
                            .like("spu_name", key));
        }
        String status = (String) params.get("status");
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq("publish_status", status);
        }
        String brandId = (String) params.get("brandId");
        if (StringUtils.isNotBlank(brandId) && !"0".equalsIgnoreCase(brandId)) {
            wrapper.eq("brand_id", brandId);
        }
        String catelogId = (String) params.get("catelogId");
        if (StringUtils.isNotBlank(catelogId) && !"0".equalsIgnoreCase(catelogId)) {
            wrapper.eq("catalog_id", catelogId);
        }
        // 可从上面原生的写法搬过来修改
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }

    /**
     * 发布
     *
     * @param spuId spu id
     */
    @Override
    public void publish(Long spuId) {
        // 查出当前 spuId 对应的所有sku 信息、品牌名字、属性 [2 运行内存 * 4 颜色= 8 个sku]
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);
        // NOTE: 4 sku 规格属性中是否可被检索，甚至8个sku是一样的，查8遍 > 需要踢出循环 > 因为 sku 继承自 spu，所以从上往下找
        // pms_product_attr_vale
        List<ProductAttrValueEntity> productAttrValueEntities = productAttrValueService.baseAttrListForSpu(spuId);
        // pms_attr 一次筛选查表保存结果： 通过 Dao 查询拿到可搜索的 attrIds
        List<Long> attrIds = productAttrValueEntities.stream()
                .map(ProductAttrValueEntity::getAttrId).collect(Collectors.toList());
        List<Long> searchAttrIds = attrService.selectSearchAttrIds(attrIds);
        // 组合筛选：利用集合不重复特性，结合 filter 流是否包含的判断（似乎性能比直接遍历快）; 创建时放入容器直接转化
        Set<Long> attrIdsSet = new HashSet<>(searchAttrIds);
        List<SkuEsModel.Attrs> attrsList = productAttrValueEntities.stream()
                .filter(entity -> attrIdsSet.contains(entity.getAttrId()))
                .map(entity -> {
                    SkuEsModel.Attrs attrs1 = new SkuEsModel.Attrs();
                    BeanUtils.copyProperties(entity, attrs1);
                    return attrs1;
                }).collect(Collectors.toList());
        // NOTE: 1 发送远程调用，库存系统查询是否有库存（非精确查具体数字）
        // 远程调用可能失败，阻塞下面的进程，所以 try catch 保证后续执行
        List<Long> skuIds = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        Map<Long, Boolean> skuHasStockMap = null;
        try {
            R skuHasStock = wareFeignService.getSkuHasStock(skuIds);
            // 受保护的对象，所以要写成实体类来调用
            TypeReference<List<SkuHasStockVo>> typeReference = new TypeReference<List<SkuHasStockVo>>() {};
            skuHasStockMap = skuHasStock.getData(typeReference).stream()
                    .collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
        } catch (Exception e) {
            log.error("SpuInfoService 调用远程查询是否有库存时报错：", e);
        }
        // 封装 sku 信息
        Map<Long, Boolean> finalSkuHasStockMap = skuHasStockMap;
        List<SkuEsModel> skuEsModels = skus.stream().map(sku -> {
            SkuEsModel esModel = new SkuEsModel();
            // pms_sku_info 属性对拷解决大部分，手动赋值解决小部分
            BeanUtils.copyProperties(sku, esModel);
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());
            // 1️⃣ 刚开始占位防报错，可肌肉记忆地写“零”相关的默认值，false，null，...；远程调用8次，很慢；
            if (CollectionUtils.isEmpty(finalSkuHasStockMap)) {
                esModel.setHasStock(true);
            } else {
                esModel.setHasStock(finalSkuHasStockMap.get(sku.getSkuId()));
            }
            // NOTE: 2 热度评分（刚上架置顶等, 待拓展，暂时默认为0）
            esModel.setHotScore(0L);
            // NOTE: 3 品牌和分类的信息 pms_brand
            BrandEntity brandEntity = brandService.getById(sku.getBrandId());
            esModel.setBrandName(brandEntity.getName());
            esModel.setBrandImg(brandEntity.getLogo());
            // pms_category
            CategoryEntity categoryEntity = categoryService.getById(sku.getCatalogId());
            esModel.setCatalogName(categoryEntity.getName());
            // 4️⃣ 设置检索属性
            esModel.setAttrs(attrsList);
            return esModel;
        }).collect(Collectors.toList());
        // NOTE: 5 数据打包发送给 elasticsearch 微服务保存
        // 返回结果分情况处理
        R esResponse = searchFeignService.publishProduct(skuEsModels);
        if (esResponse.isOk()) {
            // NOTE: 更新物品上架状态; 一次查表搞定，含系统日期，不用原生 Mybatis 方法（调用2次）
            baseMapper.updateSpuStatus(spuId, ProductConstant.PublishStatusEnum.SPU_UP.getCode());
        } else {
            // TODO: 7 接口幂等性（重复调用）；重试机制？
        }
    }


}