package cn.miozus.gulimall.seckill.feign;

import cn.miozus.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 产品微服务之间服务
 *
 * @author miozus
 * @date 2022/03/09
 */
@FeignClient("gulimall-product")

public interface ProductFeignService {
    @RequestMapping("/product/skuinfo/info/{skuId}")
    public R querySkuInfo(@PathVariable("skuId") Long skuId);
}
