package cn.miozus.gulimall.order.feign;

import cn.miozus.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 器皿装服务
 *
 * @author miao
 * @date 2022/01/18
 */
@FeignClient("gulimall-ware")
public interface WareFeignService {
    /**
     * 批量查询商品是否有库存
     *
     * @param skuIds sku id
     * @return {@link R}
     */
    @PostMapping("/ware/waresku/hasStock")
    public R querySkuHasStock(@RequestBody List<Long> skuIds);

}
