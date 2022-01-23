package cn.miozus.gulimall.ware.service.impl;

import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.common.utils.R;
import cn.miozus.gulimall.ware.dao.WareSkuDao;
import cn.miozus.gulimall.ware.entity.WareSkuEntity;
import cn.miozus.common.exception.NoStockException;
import cn.miozus.gulimall.ware.feign.ProductFeignService;
import cn.miozus.gulimall.ware.service.WareSkuService;
import cn.miozus.gulimall.ware.vo.*;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 为某个订单锁定库存
     * 收集数据 + 分析数据
     * 简单：每个成功，才代表所有成功; 只要有一个失败，就提前返回 False
     * Transactional: 默认运行时异常回滚，所以省略value，在Controller层回滚
     *
     * @param wareSkuLockVo 锁定库存传输必备信息
     * @return {@link List}<{@link LockStockResult}>
     */
    @Override
    @Transactional(rollbackFor=NoStockException.class)
    public boolean lockOrderStock(WareSkuLockVo wareSkuLockVo) throws NoStockException {
        boolean skuStocked = false;
        List<SkuWareHasStock> skuWareHasStocks = queryWareIdsBySkuId(wareSkuLockVo);

        for (SkuWareHasStock stock : skuWareHasStocks) {
            Long skuId = stock.getSkuId();
            List<Long> wareIds = stock.getWareIds();
            if (CollectionUtils.isEmpty(wareIds)) {
                throw new NoStockException(skuId);
            }
            Integer num = stock.getNum();
            for (Long wareId : wareIds) {
                Long update = wareSkuDao.updateStockLock(skuId, wareId, num);
                if (update == 1) {
                    skuStocked = true;
                    break;
                }
                // 仓库锁失败，尝试下一个仓库
            }
            if (!skuStocked) {
                throw new NoStockException(skuId);
            }

        }
        return true;
    }

    private List<SkuWareHasStock> queryWareIdsBySkuId(WareSkuLockVo wareSkuLockVo) {
        List<OrderItemVo> orderItems = wareSkuLockVo.getOrderItems();
        return orderItems.stream().map(item -> {
            SkuWareHasStock skuWareHasStock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            List<Long> wareIds = wareSkuDao.queryWareIdsBySkuId(skuId);
            skuWareHasStock.setSkuId(skuId);
            skuWareHasStock.setWareIds(wareIds);
            skuWareHasStock.setNum(item.getCount());
            return skuWareHasStock;
        }).collect(Collectors.toList());
    }

}