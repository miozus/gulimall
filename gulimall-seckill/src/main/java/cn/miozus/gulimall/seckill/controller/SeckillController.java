package cn.miozus.gulimall.seckill.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.seckill.service.SeckillService;
import cn.miozus.gulimall.seckill.to.SeckillSkuRedisTo;
import com.alibaba.cloud.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * 秒杀控制器
 *
 * @author miozus
 * @date 2022/03/16
 */

@Slf4j
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
        log.debug("seckillSku: may NPE", to);
        if (Objects.isNull(to)) {
            return R.error();
        }
        return R.ok().setData(to);
    }

    /**
     * 添加到秒杀流程
     *
     * @param killId 秒杀商品缓存键 sessionId_skuId
     * @param key    随机码 randomCode
     * @param num    数量
     * @return {@link R}
     */
    @GetMapping("/kill")
    public R addToSeckill(
            @RequestParam("killId") String killId,
            @RequestParam("key") String key,
            @RequestParam("num") Integer num) {

        TimeInterval timer = DateUtil.timer();
        String orderSn = seckillService.kill(killId, key, num);
        log.info("秒杀创建订单用时：" + timer.interval());
        System.out.println("🎊 seckill orderSn = " + orderSn);
        if (StringUtils.isEmpty(orderSn)) {
            return R.error();
        }
        return R.ok().setData(orderSn);
    }


}
