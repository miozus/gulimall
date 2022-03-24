package cn.miozus.gulimall.product.feign;

import cn.miozus.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 秒杀远程调用服务
 *
 * @author Miozus
 * @date 2022/03/24
 */
@FeignClient("gulimall-seckill")
public interface SeckillFeignService {


    /**
     * 获取秒杀场次信息
     *
     * @param skuId sku id
     * @return {@link R}
     */
    @GetMapping("/sku/seckill/{skuId}")
    R fetchSeckillSku(@PathVariable("skuId") Long skuId);
}
