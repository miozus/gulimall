package cn.miozus.gulimall.product.service.impl;

import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.gulimall.product.dao.SkuInfoDao;
import cn.miozus.gulimall.product.entity.SkuImagesEntity;
import cn.miozus.gulimall.product.entity.SkuInfoEntity;
import cn.miozus.gulimall.product.entity.SpuInfoDescEntity;
import cn.miozus.gulimall.product.service.*;
import cn.miozus.gulimall.product.vo.SkuItemVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * sku信息服务
 *
 * @author miao
 * @date 2021/12/24
 */
@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    SkuImagesService skuImagesService;
    @Autowired
    SpuInfoDescService spuInfoDescService;
    @Autowired
    ProductAttrValueService productAttrValueService;
    @Autowired
    AttrGroupService attrGroupService;
    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);

    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        /*
        * key:
        catalogId: 0
        brandId: 0
        min: 0
        max: 0
        * */
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (StringUtils.isNotBlank(key)) {
            wrapper.and(
                    w -> w.eq("sku_id", key).or().like("sku_name", key)
            );
        }
        String brandId = (String) params.get("brandId");
        if (StringUtils.isNotBlank(brandId) && !"0".equalsIgnoreCase(brandId)) {
            wrapper.eq("brand_id", brandId);
        }
        String catalogId = (String) params.get("catalogId");
        if (StringUtils.isNotBlank(catalogId) && !"0".equalsIgnoreCase(catalogId)) {
            wrapper.eq("catalog_id", catalogId);
        }
        String min = (String) params.get("min");
        if (StringUtils.isNotBlank(min)) {
            wrapper.ge("price", min);
        }
        String max = (String) params.get("max");
        if (StringUtils.isNotBlank(max)) {
            // 默认值为零，需要手动修改才能查询
            try {
                // try 避免用户传入字符串的转换失败
                BigDecimal bigDecimal = new BigDecimal(max);
                if (bigDecimal.compareTo(BigDecimal.ZERO) > 0) {

                    wrapper.le("price", max);
                }
            } catch (Exception e) {
                log.error("sku手动设定最大值时错误: {}", e);

            }
        }
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        return this.list(
                new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId)
        );
    }

    /**
     * 封装整个商品详情页的数据
     * <p>
     * 1.sku 基本信息
     * 2.sku 图片
     * 3.sku 销售属性: 属性组合 > 动态切换 URL
     * 4.spu 商品介绍: 所有的描述（一张长图）
     * 5.sku 规格与包装：(分组 > 商品规格属性键值对)*N
     * <p>
     * 异步编排依赖先后顺序，收尾时可省略最底层的：
     * 1 > spuId > 3, 4
     * 1 > spuId, catalogId > 5
     * 2
     *
     * @param skuId sku id
     * @return {@link SkuItemVo}
     */
    @SneakyThrows
    @Override
    public SkuItemVo item(Long skuId) {
        SkuItemVo vo = new SkuItemVo();

        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfoEntity info = getById(skuId);
            vo.setInfo(info);
            return info;
        }, executor);

        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync((info) -> {
            SpuInfoDescEntity desc = spuInfoDescService.getById(info.getSpuId());
            vo.setDesc(desc);
        }, executor);

        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync((info) -> {
            List<SkuItemVo.SkuItemSaleAttrVo> saleAttr = skuSaleAttrValueService.querySaleAttrsBySkuId(info.getSpuId());
            vo.setSaleAttr(saleAttr);
        }, executor);

        CompletableFuture<Void> groupAttrFuture = infoFuture.thenAcceptAsync((info) -> {
            Long spuId = info.getSpuId();
            Long catalogId = info.getCatalogId();
            List<SkuItemVo.SpuItemGroupAttrVo> groupAttrVo = attrGroupService.queryAttrGroupWithAttrsBySpuId(spuId, catalogId);
            vo.setGroupAttrs(groupAttrVo);
        }, executor);

        CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(() -> {
            List<SkuImagesEntity> images = skuImagesService.getImagesBySkuId(skuId);
            vo.setImages(images);
        }, executor);

        CompletableFuture.allOf(descFuture, saleAttrFuture, groupAttrFuture, imagesFuture).get();

        return vo;
    }

}