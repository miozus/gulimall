package cn.miozus.gulimall.ware.service;

import cn.miozus.common.exception.GuliMallBindException;
import cn.miozus.common.to.mq.OrderTo;
import cn.miozus.common.to.mq.StockLockedUndoLogTo;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.gulimall.ware.entity.WareOrderTaskDetailEntity;
import cn.miozus.gulimall.ware.entity.WareOrderTaskEntity;
import cn.miozus.gulimall.ware.entity.WareSkuEntity;
import cn.miozus.gulimall.ware.vo.SkuHasStockVo;
import cn.miozus.gulimall.ware.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:20:54
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryWareSkuPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> querySkuHasStock(List<Long> skuIds);

    /**
     * 锁定库存，并回执每个商品锁定结果
     *
     * @param wareSkuLockVo 库存 sku 锁定数据
     * @return boolean
     * @throws GuliMallBindException 谷粒商城异常
     */
    boolean lockOrderStock(WareSkuLockVo wareSkuLockVo) throws GuliMallBindException;

    /**
     * 解锁库存：只有正常关闭的订单，会解锁
     *
     * @param to 传输数据
     * @throws RuntimeException 远程调用失败异常
     */
    void unlockOrderStock(StockLockedUndoLogTo to) throws RuntimeException;

    /**
     * 解锁库存：关闭订单后，排除时差影响的二次确认
     *
     * @param to 30分钟后传过来的数据
     */
    void unlockOrderStock(OrderTo to);

    /**
     * 构建库存锁定回滚日志
     *
     * @param task        库存任务
     * @param stockDetail 库存细节
     * @return {@link StockLockedUndoLogTo}
     */
    StockLockedUndoLogTo buildStockLockedUndoLogTo(WareOrderTaskEntity task, WareOrderTaskDetailEntity stockDetail);
}

