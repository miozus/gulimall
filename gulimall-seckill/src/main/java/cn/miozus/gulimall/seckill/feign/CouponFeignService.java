package cn.miozus.gulimall.seckill.feign;

import cn.miozus.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 优惠券微服务之间服务
 *
 * @author miozus
 * @date 2022/03/07
 */
@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    @GetMapping("/coupon/seckillsession/last3dSession")
    public R queryLast3dSession();

}
