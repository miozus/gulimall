package cn.miozus.gulimall.cart.feign;

import cn.miozus.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

/**
 * 产品装服务
 *
 * @author miao
 * @date 2022/01/10
 */
@FeignClient("gulimall-product")
public interface ProductFeignService {

    /**
     * 查询sku信息通过id
     *
     * @param skuId sku id
     * @return {@link R}
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    R querySkuInfoById(@PathVariable("skuId") Long skuId);

    /**
     * 查询sku attrs
     *
     * @param skuId sku id
     * @return {@link List}<{@link String}>
     */
    @GetMapping("/product/skusaleattrvalue/stringlist/{skuId}")
    List<String> querySkuAttrs(@PathVariable("skuId") Long skuId);

    /**
     * 查询sku价格
     *
     * @param skuId sku id
     * @return {@link BigDecimal}
     */
    @GetMapping("/product/skuinfo/{skuId}/price")
    BigDecimal querySkuPrice(@PathVariable("skuId") Long skuId);


}
