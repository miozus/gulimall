package cn.miozus.gulimall.ware.service.impl;

import cn.miozus.gulimall.common.annotation.PostRabbitMq;
import cn.miozus.gulimall.common.constant.WareConstant;
import cn.miozus.gulimall.common.enume.BizCodeEnum;
import cn.miozus.gulimall.common.enume.OrderStatusEnum;
import cn.miozus.gulimall.common.exception.GuliMallBindException;
import cn.miozus.gulimall.common.to.mq.OrderTo;
import cn.miozus.gulimall.common.to.mq.StockDetailTo;
import cn.miozus.gulimall.common.to.mq.StockLockedUndoLogTo;
import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.common.utils.Query;
import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.ware.dao.WareSkuDao;
import cn.miozus.gulimall.ware.entity.WareOrderTaskDetailEntity;
import cn.miozus.gulimall.ware.entity.WareOrderTaskEntity;
import cn.miozus.gulimall.ware.entity.WareSkuEntity;
import cn.miozus.gulimall.ware.feign.OrderFeignService;
import cn.miozus.gulimall.ware.feign.ProductFeignService;
import cn.miozus.gulimall.ware.service.WareOrderTaskDetailService;
import cn.miozus.gulimall.ware.service.WareOrderTaskService;
import cn.miozus.gulimall.ware.service.WareSkuService;
import cn.miozus.gulimall.ware.vo.*;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;
    @Autowired
    WareOrderTaskService wareOrderTaskService;
    @Autowired
    OrderFeignService orderFeignService;
    @Autowired
    private WareSkuService wareSkuService;

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

    /**
     * 查询sku有库存
     * <p>
     * 查询当前 sku 总库存量
     * SELECT SUM(stock-stock_locked) FROM wms_ware_sku WHERE sku_id = 1
     * 🐞 返回类型应为 包装类，因为范畴容许 null 类型
     *
     * @param skuIds sku id
     * @return {@link List}<{@link SkuHasStockVo}>
     */
    @Override
    public List<SkuHasStockVo> querySkuHasStock(List<Long> skuIds) {
        return skuIds.stream().map(skuId -> {
                    SkuHasStockVo vo = new SkuHasStockVo();
                    Long count = baseMapper.getSkuStock(skuId);
                    vo.setSkuId(skuId);
                    vo.setHasStock(count != null && count > 0);
                    return vo;
                }
        ).collect(Collectors.toList());
    }


    /**
     * 解锁库存流程
     *           ┌─────────────┐
     *        ┌──┤StockSnapshot│
     *        │  └─────────────┘
     *        │            null  0
     *        │          exists  1 ──► StockLockedStatus
     * UndoLog│
     *        │                              locked 1──┐
     *        │                            unlocked 0 ◄├──┐
     *        │      ┌─────────┐         subtracted 0  │  │
     *        └──────┤OrderTask│                       │  │
     *               └┬────────┘                       │  │
     *                │                                │  │
     *                │  ┌─────┐                       │  │
     *                └─►│Order│     GlobalOrderStatus │  │
     *                   └─────┘                       │  │
     *              ┌────  null   1  failed ◄──────────┘  │
     *              │ exception 0.5  waitingPay/error     │
     *              │ │  exists   0  success              │
     *              ▼ ▼                                   │
     *            unlock──────────────────────────────────┘
     * <p>
     * 先查快照表记录
     * * 无：无需解锁
     * * 有：状态未锁定，无需解锁
     * <p>
     * 再查询全局订单记录
     * * 有：成功/待支付，无需解锁
     * * 无：（自动到时）关闭/方法异常，需要回滚，并签收消息
     * * 其他：网络调用故障，需要回滚解锁，拒收消息
     *
     * @param to 快照传输数据
     */
    @Override
    public void unlockOrderStock(StockLockedUndoLogTo to) throws RuntimeException {
        Long taskId = to.getOrderTaskId();
        StockDetailTo snapshot = to.getStockDetailSnapshot();
        Long snapshotDetailId = snapshot.getId();
        WareOrderTaskDetailEntity detail = wareOrderTaskDetailService.getById(snapshotDetailId);
        if (Objects.isNull(detail) || detail.getLockStatus() != WareConstant.LockedStatusEnum.LOCKED.getCode()) {
            return;
        }
        WareOrderTaskEntity stockTask = wareOrderTaskService.getById(taskId);
        String orderSn = stockTask.getOrderSn();
        R r = orderFeignService.queryOrderBySn(orderSn);
        if (r.isNotOk()) {
            throw new GuliMallBindException(BizCodeEnum.FEIGN_READ_TIMEOUT_EXCEPTION);
        }
        OrderVo data = r.getData(new TypeReference<OrderVo>() {
        });
        if (Objects.isNull(data) || data.getStatus() == OrderStatusEnum.CANCELED.getCode()) {
            unlockOrderStockBySql(detail, snapshotDetailId);
        }
    }

    /**
     * 解锁库存：关闭订单后，排除时差影响的二次确认
     *
     * @param to 来
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockOrderStock(OrderTo to) {
        String orderSn = to.getOrderSn();
        WareOrderTaskEntity stockTask = wareOrderTaskService.getOne(
                new QueryWrapper<WareOrderTaskEntity>().eq("order_sn", orderSn)
        );
        Long taskId = stockTask.getId();
        List<WareOrderTaskDetailEntity> details = wareOrderTaskDetailService.list(
                new QueryWrapper<WareOrderTaskDetailEntity>()
                        .eq("task_id", taskId)
                        .eq("lock_status", WareConstant.LockedStatusEnum.LOCKED.getCode())
        );
        for (WareOrderTaskDetailEntity detail : details) {
            unlockOrderStockBySql(detail, taskId);
        }


    }

    private void unlockOrderStockBySql(WareOrderTaskDetailEntity detail, Long detailId) {
        wareSkuDao.updateStockBackToLastStatus(detail.getSkuId(), detail.getWareId(), detail.getSkuNum());
        WareOrderTaskDetailEntity entity = WareOrderTaskDetailEntity.builder()
                .id(detailId)
                .lockStatus(WareConstant.LockedStatusEnum.UNLOCKED.getCode()).build();
        wareOrderTaskDetailService.updateById(entity);
    }

    /**
     * 为某个订单锁定库存
     * 收集数据 + 分析数据
     * 简单：每个成功，才代表所有成功; 只要有一个失败，就提前返回 False
     * Transactional: 默认运行时异常回滚，所以省略value，在Controller层回滚
     * <p>
     * task: 库存日志：锁库存成功不回滚，但全局订单失败，需要手动回滚，附加订单编号等，方便阅读排查
     * taskDetail: 库存快照: 锁定后的样子
     *
     * @param wareSkuLockVo 锁定库存传输必备信息
     * @return {@link List}<{@link LockStockResult}>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockOrderStock(WareSkuLockVo wareSkuLockVo) throws GuliMallBindException {
        boolean stockLocked = false;
        List<SkuWareHasStock> skuWareHasStocks = queryWareIdsBySkuId(wareSkuLockVo);
        WareOrderTaskEntity stockTask = buildStockLockedUndoLogTask(wareSkuLockVo);

        for (SkuWareHasStock stock : skuWareHasStocks) {
            Long skuId = stock.getSkuId();
            List<Long> wareIds = stock.getWareIds();
            if (CollectionUtils.isEmpty(wareIds)) {
                throw new GuliMallBindException(skuId + " 号（skuId）商品库存不足，请重新下单");
            }
            Integer skuNum = stock.getSkuNum();
            String skuName = stock.getSkuName();
            for (Long wareId : wareIds) {
                Long update = wareSkuDao.updateStockLock(skuId, wareId, skuNum);
                if (update == 1) {
                    // 锁定成功，保存库存最新快照
                    WareOrderTaskDetailEntity stockDetail = saveStockLockedSnapshot(stockTask, skuId, skuName, skuNum, wareId);
                    // 发送锁定库存消息至延时队列
                    wareSkuService.buildStockLockedUndoLogTo(stockTask, stockDetail);
                    stockLocked = true;
                    break;
                }
                // 仓库锁失败，尝试下一个仓库
            }
            // 全局失败则回滚
            if (!stockLocked) {
                throw new GuliMallBindException(skuId + " 号（skuId）商品库存不足，请重新下单");
            }

        }
        return true;
    }

    private WareOrderTaskDetailEntity saveStockLockedSnapshot(WareOrderTaskEntity task, Long skuId, String skuName, Integer num, Long wareId) {
        WareOrderTaskDetailEntity stockDetail = WareOrderTaskDetailEntity.builder()
                .skuId(skuId)
                .skuName(skuName)
                .skuNum(num)
                .lockStatus(1)
                .wareId(wareId)
                .taskId(task.getId()).build();
        wareOrderTaskDetailService.save(stockDetail);
        return stockDetail;
    }

    private WareOrderTaskEntity buildStockLockedUndoLogTask(WareSkuLockVo wareSkuLockVo) {
        WareOrderTaskEntity task = new WareOrderTaskEntity();
        task.setOrderSn(wareSkuLockVo.getOrderSn());
        task.setCreateTime(new Date());
        wareOrderTaskService.save(task);
        return task;
    }

    @Override
    @PostRabbitMq("库存锁定快照")
    public StockLockedUndoLogTo buildStockLockedUndoLogTo(WareOrderTaskEntity task, WareOrderTaskDetailEntity stockDetail) {
        StockLockedUndoLogTo undoLogTo = new StockLockedUndoLogTo();
        undoLogTo.setOrderTaskId(task.getId());
        StockDetailTo stockDetailTo = new StockDetailTo();
        BeanUtils.copyProperties(stockDetail, stockDetailTo);
        undoLogTo.setStockDetailSnapshot(stockDetailTo);
        return undoLogTo;
    }

    private List<SkuWareHasStock> queryWareIdsBySkuId(WareSkuLockVo wareSkuLockVo) {
        List<OrderItemVo> orderItems = wareSkuLockVo.getOrderItems();
        return orderItems.stream().map(item -> {
            Long skuId = item.getSkuId();
            List<Long> wareIds = wareSkuDao.queryWareIdsBySkuId(skuId);
            return SkuWareHasStock.builder()
                    .skuId(skuId)
                    .skuNum(item.getCount())
                    .skuName(item.getTitle())
                    .wareIds(wareIds).build();
        }).collect(Collectors.toList());
    }

}