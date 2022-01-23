package cn.miozus.gulimall.order.service;

import cn.miozus.common.exception.NoStockException;
import cn.miozus.gulimall.order.to.OrderCreateTo;
import cn.miozus.gulimall.order.vo.OrderConfirmVo;
import cn.miozus.gulimall.order.vo.OrderSubmitRespVo;
import cn.miozus.gulimall.order.vo.OrderSubmitVo;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.gulimall.order.entity.OrderEntity;

import java.util.Map;

/**
 * 订单
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:18:03
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 结算页渲染数据
     *
     * @return {@link OrderConfirmVo}
     */
    OrderConfirmVo confirmOrder();

    /**
     * 提交订单
     *
     * @param orderSubmitVo 订单提交签证官
     * @return {@link OrderSubmitRespVo}
     */
    OrderSubmitRespVo submitOrder(OrderSubmitVo orderSubmitVo) throws NoStockException;

    /**
     * 创建订单
     *
     * @return {@link OrderCreateTo}
     */
    OrderCreateTo createOrder();
}

