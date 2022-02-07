package cn.miozus.gulimall.member.feign;

import cn.miozus.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 订单装服务
 *
 * @author miao
 * @date 2022/01/28
 */
@FeignClient("gulimall-order")
public interface OrderFeignService {

    /**
     * 列表，包含购物车商品详情
     * RequestBody: JSON 格式传输比TEXT 好
     *
     * @param params 参数个数
     * @return {@link R}
     */
    @PostMapping("/order/order/listWithItems")
    public R listWithItems(@RequestBody Map<String, Object> params);
}
