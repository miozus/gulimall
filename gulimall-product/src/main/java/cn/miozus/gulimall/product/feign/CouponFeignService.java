package cn.miozus.gulimall.product.feign;

import cn.miozus.gulimall.common.to.SkuReductionTo;
import cn.miozus.gulimall.common.to.SpuBoundTo;
import cn.miozus.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    /**
     * 保存spu界限
     *   1) @RequestBody 将对象转换为 JSON
     *   2）找到微服务，给URI发送请求，将JSON放在请求体位置，发送请求
     *   3）对方微服务收到请求，请求体内有 JSON 数据
     *     (@RequestBody (->将JSON转换为->） SpuBoundsEntity spuBounds)
     *     没有死规定要一模一样，形式参数传过去也能转
     *   只要 JSON 数据模块兼容，双方微服务无需使用同一个To
     *   即，为了开发方便，直接将 Controller 复制到这里
     *
     * @param spuBoundTo spu绑定到
     * @return {@link R}
     */
    @PutMapping("coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SpuBoundTo spuBoundTo);


    /**
     * 保存sku减少
     *
     * @param skuReductionTo sku减少
     * @return {@link R}
     */
    @PutMapping("coupon/skufullreduction/saveinfo")
    R saveSkuReduction(@RequestBody SkuReductionTo skuReductionTo);
}
