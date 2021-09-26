package cn.miozus.gulimall.ware.feign;

import cn.miozus.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    /**
     * 信息
     *  I 直接给后台微服务发请求
     *     /product/skuinfo/info/{skuId}
     *      @FeignClient("gulimall-product")
     *  II 所有请求过网关，转发获取请求
     *     /api/product/skuinfo/info/{skuId}
     *     @FeignClient("gulimall-gateway")
     *
     * @param skuId sku id
     * @return {@link R}
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    R info(@PathVariable("skuId") Long skuId);

}