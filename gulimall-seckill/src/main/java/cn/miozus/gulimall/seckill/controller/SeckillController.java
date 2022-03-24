package cn.miozus.gulimall.seckill.controller;

import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.seckill.service.SeckillService;
import cn.miozus.gulimall.seckill.to.SeckillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * 秒杀控制器
 *
 * @author miozus
 * @date 2022/03/16
 */

@RestController
public class SeckillController {

    @Autowired
    SeckillService seckillService;

    /**
     * 得到当前时间可以参与的所有秒杀商品信息
     *
     * @return {@link R}
     */
    @GetMapping("/currentSeckillSkus")
    public R getCurrentSeckillSkus() {
        List<SeckillSkuRedisTo> tos = seckillService.fetchCurrentSeckillSkus();
        return R.ok().setData(tos);
    }

    /**
     * 查询单个商品携带的秒杀信息
     *
     * @param skuId sku id
     * @return {@link R}
     */
    @GetMapping("/sku/seckill/{skuId}")
    public R fetchSeckillSku(@PathVariable("skuId") Long skuId) {
        SeckillSkuRedisTo to = seckillService.fetchSeckillSku(skuId);
        if (Objects.isNull(to.getId())) {
            return R.error();
        }
        return R.ok().setData(to);
    }


}
