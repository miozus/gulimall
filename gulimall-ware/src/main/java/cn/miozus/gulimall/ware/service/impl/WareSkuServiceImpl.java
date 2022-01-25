package cn.miozus.gulimall.ware.service.impl;

import cn.miozus.common.constant.OrderConstant;
import cn.miozus.common.constant.WareConstant;
import cn.miozus.common.exception.FeignDeliverException;
import cn.miozus.common.exception.NoStockException;
import cn.miozus.common.to.stock.StockDetailTo;
import cn.miozus.common.to.stock.StockLockedUndoLogTo;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.common.utils.R;
import cn.miozus.gulimall.ware.config.RabbitMqConfig;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    RabbitTemplate rabbitTemplate;
    @Autowired
    OrderFeignService orderFeignService;

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
     *
     * 先查快照表记录
     * 无：无需解锁
     * 有：状态未锁定，无需解锁
     *
     * 再查询全局订单记录
     * *
     * * 有：成功/待支付，无需解锁
     * * 无：（自动到时）关闭/方法异常，需要回滚，并签收消息
     * * 其他：网络调用故障，需要回滚解锁，拒收消息
     * @param to 来
     */
    @Override
    public void unlockOrderStock(StockLockedUndoLogTo to) throws RuntimeException {
        Long taskId = to.getOrderTaskId();
        StockDetailTo snapshot = to.getStockDetailSnapshot();
        Long snapshotDetailId = snapshot.getId();
        WareOrderTaskDetailEntity detail = wareOrderTaskDetailService.getById(snapshotDetailId);
        if (Objects.isNull(detail) || detail.getLockStatus() != WareConstant.StockLockedStatusEnum.LOCKED.getCode()) {
            return;
        }
        WareOrderTaskEntity stockTask = wareOrderTaskService.getById(taskId);
        String orderSn = stockTask.getOrderSn();
        R r = orderFeignService.queryOrderBySn(orderSn);
        if (r.getCode() != 0) {
            throw new FeignDeliverException();
        }
        OrderVo data = r.getData(new TypeReference<OrderVo>() {
        });
        if (Objects.isNull(data) || data.getStatus() == OrderConstant.StatusEnum.CLOSED.code) {
            unlockOrderStockBySql(detail, snapshotDetailId);
        }
    }

    private void unlockOrderStockBySql(WareOrderTaskDetailEntity detail, Long detailId) {
        wareSkuDao.updateStockBackToLastStatus(detail.getSkuId(), detail.getWareId(), detail.getSkuNum());
        WareOrderTaskDetailEntity entity = WareOrderTaskDetailEntity.builder()
                .id(detailId)
                .lockStatus(WareConstant.StockLockedStatusEnum.UNLOCKED.getCode()).build();
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
    @Transactional(rollbackFor = NoStockException.class)
    public boolean lockOrderStock(WareSkuLockVo wareSkuLockVo) throws NoStockException {
        boolean stockLocked = false;
        List<SkuWareHasStock> skuWareHasStocks = queryWareIdsBySkuId(wareSkuLockVo);
        WareOrderTaskEntity stockTask = buildStockUndoLogTask(wareSkuLockVo);

        for (SkuWareHasStock stock : skuWareHasStocks) {
            Long skuId = stock.getSkuId();
            List<Long> wareIds = stock.getWareIds();
            if (CollectionUtils.isEmpty(wareIds)) {
                throw new NoStockException(skuId);
            }
            Integer skuNum = stock.getSkuNum();
            String skuName = stock.getSkuName();
            for (Long wareId : wareIds) {
                Long update = wareSkuDao.updateStockLock(skuId, wareId, skuNum);
                if (update == 1) {
                    // 锁定成功，保存库存最新快照
                    WareOrderTaskDetailEntity stockDetail = saveStockLockedSnapshot(stockTask, skuId, skuName, skuNum, wareId);
                    // 发送锁定库存消息至延时队列
                    pushStockLockedMq(stockTask, stockDetail);
                    stockLocked = true;
                    break;
                }
                // 仓库锁失败，尝试下一个仓库
                int i = 10 /0;
            }
            // 全局失败则回滚
            if (!stockLocked) {
                throw new NoStockException(skuId);
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

    private WareOrderTaskEntity buildStockUndoLogTask(WareSkuLockVo wareSkuLockVo) {
        WareOrderTaskEntity task = new WareOrderTaskEntity();
        task.setOrderSn(wareSkuLockVo.getOrderSn());
        task.setCreateTime(new Date());
        wareOrderTaskService.save(task);
        return task;
    }

    private void pushStockLockedMq(WareOrderTaskEntity task, WareOrderTaskDetailEntity stockDetail) {
        StockLockedUndoLogTo undoLogTo = new StockLockedUndoLogTo();
        undoLogTo.setOrderTaskId(task.getId());
        StockDetailTo stockDetailTo = new StockDetailTo();
        BeanUtils.copyProperties(stockDetail, stockDetailTo);
        undoLogTo.setStockDetailSnapshot(stockDetailTo);
        rabbitTemplate.convertAndSend(RabbitMqConfig.STOCK_EVENT_EXCHANGE,
                RabbitMqConfig.STOCK_DELAY_QUEUE_ROUTING_KEY,
                undoLogTo);
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