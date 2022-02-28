package cn.miozus.gulimall.member.feign;

import cn.miozus.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

// 以你之名，声明式远程调用请求
@FeignClient("gulimall-coupon")
public interface CouponFeignService {
    // 路径：（模块名/实体类） + 方法（方法名/返回类型）
    @RequestMapping("coupon/coupon/member/list")
    public R memberCoupons();

}

