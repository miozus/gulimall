package cn.miozus.gulimall.seckill.service.impl;

import cn.miozus.gulimall.common.annotation.GetRedis;
import cn.miozus.gulimall.common.annotation.PutRedis;
import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.seckill.feign.CouponFeignService;
import cn.miozus.gulimall.seckill.service.SeckillService;
import cn.miozus.gulimall.seckill.to.SeckillSkuRedisTo;
import cn.miozus.gulimall.seckill.vo.SeckillSessionWithSkus;
import com.alibaba.fastjson.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 秒杀服务impl
 *
 * @author miozus
 * @date 2022/03/16
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;
    @Autowired
    private SeckillService seckillService;

    @Override
    public void uploadContinuous3DaySku() {
        R r = couponFeignService.queryLast3dSession();
        if (r.getCode() == 0) {
            List<SeckillSessionWithSkus> sessionData = r.getData(new TypeReference<List<SeckillSessionWithSkus>>() {
            });
            seckillService.saveSessionDataRedis(sessionData);
        }

    }

    @Override
    @PutRedis("保存秒杀商品信息")
    public void saveSessionDataRedis(List<SeckillSessionWithSkus> sessionData) {

    }

    @Override
    @GetRedis("查询秒杀商品信息")
    public List<SeckillSkuRedisTo> fetchSeckillSkuInfo() {
        return Collections.emptyList();
    }
}
