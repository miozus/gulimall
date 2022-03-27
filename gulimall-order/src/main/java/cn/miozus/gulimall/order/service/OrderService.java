package cn.miozus.gulimall.order.service;

import cn.miozus.gulimall.common.exception.GuliMallBindException;
import cn.miozus.gulimall.common.to.mq.SeckillOrderTo;
import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.order.entity.OrderEntity;
import cn.miozus.gulimall.order.to.OrderCreateTo;
import cn.miozus.gulimall.order.vo.OrderConfirmVo;
import cn.miozus.gulimall.order.vo.OrderSubmitVo;
import cn.miozus.gulimall.order.vo.PayAsyncVo;
import cn.miozus.gulimall.order.vo.PayVo;
import com.baomidou.mybatisplus.extension.service.IService;

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
     * @return {@link OrderEntity}
     * @throws GuliMallBindException 谷粒商场绑定异常
     */
    OrderEntity submitOrder(OrderSubmitVo orderSubmitVo) throws GuliMallBindException;

    /**
     * 创建订单
     *
     * @return {@link OrderCreateTo}
     */
    OrderCreateTo createOrder();

    /**
     * 查询订单状态
     *
     * @param orderSn 订单编号
     * @return {@link OrderEntity}
     */
    OrderEntity queryOrderBySn(String orderSn);

    /**
     * 关闭订单
     *
     * @param to 一个订单
     */
    void closeOrder(OrderEntity to);

    /**
     * 得到订单支付表单
     *
     * @param orderSn 订单sn
     * @return {@link PayVo}
     */
    PayVo getOrderPay(String orderSn);

    /**
     * 列表，包含购物车商品详情
     *
     * @param params 参数个数
     * @return {@link PageUtils}
     */
    PageUtils queryPageWithItems(Map<String, Object> params);

    /**
     * 处理支付结果
     *
     * @param vo 签证官
     * @return
     */
    String handlePayResult(PayAsyncVo vo);

    /**
     * 创建秒杀订单
     *
     * @param to 来
     */
    void createSeckillOrder(SeckillOrderTo to);
}

