package cn.miozus.gulimall.order.vo;

import cn.miozus.gulimall.order.entity.OrderEntity;
import lombok.Data;

/**
 * 订单提交后接收到的实体类
 *
 * @author miao
 * @date 2022/01/21
 */
@Data
public class OrderSubmitRespVo {
    private OrderEntity order;
    /** 下单回执：0表示成功 */
    private Integer code;
}
