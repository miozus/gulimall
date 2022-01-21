package cn.miozus.gulimall.ware.service.impl;

import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.common.utils.R;
import cn.miozus.gulimall.ware.dao.WareSkuDao;
import cn.miozus.gulimall.ware.entity.WareSkuEntity;
import cn.miozus.gulimall.ware.feign.ProductFeignService;
import cn.miozus.gulimall.ware.service.WareSkuService;
import cn.miozus.gulimall.ware.vo.SkuHasStockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author miao
 */
@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryWareSkuPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String wareId = (String) params.get("wareId");
        if (StringUtils.isNotBlank(wareId)) {
            wrapper.eq("ware_id", wareId);
        }
        String skuId = (String) params.get("skuId");
        if (StringUtils.isNotBlank(skuId)) {
            wrapper.eq("sku_id", skuId);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        // 判断是否有这个库存记录，无则新增
        Integer count = wareSkuDao.selectCount(
                new QueryWrapper<WareSkuEntity>().eq("ware_id", wareId).eq("sku_id", skuId)
        );
        if (count > 0) {
            // 更新 : SQL
            wareSkuDao.addStock(skuId, wareId, skuNum);
        } else {
            // 新增 : 手动赋值
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            // 跨服补充冗余字段远程商品名称，但要求失败也影响整体事务提交
            // I try...catch...
            // II TODO: 待高级部分讲解解锁🔓
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> skuInfo = (Map<String, Object>) info.get("skuInfo");
                // 查询成功
                if (info.getCode() == 0) {
                    wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
                }
            } catch (Exception e) {
                log.error("补充冗余字段skuName时报错: {}", e);
            }
            wareSkuDao.insert(wareSkuEntity);
        }
    }

    @Override
    public List<SkuHasStockVo> querySkuHasStock(List<Long> skuIds) {
        return skuIds.stream().map(skuId -> {
                    SkuHasStockVo vo = new SkuHasStockVo();
                    // 查询当前 sku 总库存量
                    // SELECT SUM(stock-stock_locked) FROM wms_ware_sku WHERE sku_id = 1
                    // 🐞 返回类型应为 包装类，因为范畴容许 null 类型
                    Long count = baseMapper.getSkuStock(skuId);
                    vo.setSkuId(skuId);
                    vo.setHasStock(count != null && count > 0);
                    return vo;
                }
        ).collect(Collectors.toList());
    }

}