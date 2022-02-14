package cn.miozus.gulimall.order.feign;

import cn.miozus.common.utils.R;
import cn.miozus.gulimall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 车装服务
 *
 * @author miao
 * @date 2022/01/15
 */
@FeignClient("gulimall-cart")
public interface CartFeignService {

    /**
     * 获取当前购物车条目（含最新价格）
     *
     * @return {@link List}<{@link OrderItemVo}>
     */
    @GetMapping("/cartItems")
    public List<OrderItemVo> fetchOrderCartItems();

    /**
     * 删除订单已付款的项目
     *
     * @return {@link R}
     */
    @DeleteMapping("/delCartItem")
    R deleteOrderCartItems();

}
