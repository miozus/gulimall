package cn.miozus.gulimall.order.feign;

import cn.miozus.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 调用商品服务
 *
 * @author miao
 * @date 2022/01/21
 */
@FeignClient("gulimall-product")
public interface ProductFeignService {
    /**
     * 查询spu信息按sku id
     *
     * @param skuId sku id
     * @return {@link R}
     */
    @PostMapping("/product/spuinfo/bySkuId/{skuId}")
    R querySpuInfo(@PathVariable("skuId") Long skuId);

    /**
     * 查询sku信息按sku id
     *
     * @param skuId sku id
     * @return {@link R}
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    R querySkuInfo(@PathVariable("skuId") Long skuId);


}
