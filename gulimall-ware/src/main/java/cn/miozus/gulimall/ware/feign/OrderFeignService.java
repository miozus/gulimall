package cn.miozus.gulimall.ware.feign;

import cn.miozus.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 订单装服务
 *
 * @author miao
 * @date 2022/01/24
 */
@FeignClient("gulimall-order")
public interface OrderFeignService {

    /**
     * 查询订单详情
     *
     * @param orderSn 订单流水编号
     * @return {@link R}
     */
    @GetMapping("/order/order/SN/{sn}")
    public R queryOrderBySn(@PathVariable("sn") String orderSn);

}
