package cn.miozus.gulimall.seckill.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.seckill.service.SeckillService;
import cn.miozus.gulimall.seckill.to.SeckillSkuRedisTo;
import com.alibaba.cloud.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;

/**
 * 秒杀控制器
 *
 * @author miozus
 * @date 2022/03/16
 */

@Slf4j
@Controller
public class SeckillController {

    @Autowired
    SeckillService seckillService;

    /**
     * 得到当前时间可以参与的所有秒杀商品信息
     *
     * @return {@link R}
     */
    @ResponseBody
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
    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R fetchSeckillSku(@PathVariable("skuId") Long skuId) {
        SeckillSkuRedisTo to = seckillService.fetchSeckillSku(skuId);
        if (Objects.isNull(to)) {
            log.debug("seckillSku: may NPE", to);
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
    public String addToSeckill(
            @RequestParam("killId") String killId,
            @RequestParam("key") String key,
            @RequestParam("num") Integer num,
            @RequestParam("returnUrl") String returnUrl,
            Model model, RedirectAttributes ra) {

        TimeInterval timer = DateUtil.timer();
        String orderSn = seckillService.kill(killId, key, num);
        log.info("创建秒杀订单" + " [" + orderSn + "] 耗时(毫秒)： " + timer.interval());
        if (StringUtils.isEmpty(orderSn)) {
            // 服务降级
            ra.addFlashAttribute("msg", "当前参与活动人数过多，请稍后再试");
            return "redirect:" + returnUrl;
        }
        model.addAttribute("orderSn", orderSn);
        return "success";
    }


}
