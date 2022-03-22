package cn.miozus.gulimall.seckill.controller;

import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.seckill.service.SeckillService;
import cn.miozus.gulimall.seckill.to.SeckillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 秒杀控制器
 *
 * @author miozus
 * @date 2022/03/16
 */

@Controller
public class SeckillController {

    @Autowired
    SeckillService seckillService;

    @GetMapping("/currentSeckillSkus")
    public R getCurrentSeckillSkus(){
        List<SeckillSkuRedisTo> tos = seckillService.fetchSeckillSkuInfo();
        return R.ok().setData(tos);
    }

}
