package cn.miozus.gulimall.common.to.mq;

import lombok.Data;

/**
 * 订单任务取消
 * task: 库存日志：锁库存成功不回滚，但全局订单失败，需要手动回滚，附加更多阅读信息方便排查
 *
 * @author miao
 * @date 2022/01/24
 */
@Data
public class StockLockedUndoLogTo {
    private Long orderTaskId;
    private StockDetailTo stockDetailSnapshot;
}
