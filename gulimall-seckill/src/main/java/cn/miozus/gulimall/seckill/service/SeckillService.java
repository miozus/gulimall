package cn.miozus.gulimall.seckill.service;

import cn.miozus.gulimall.seckill.to.SeckillSkuRedisTo;
import cn.miozus.gulimall.seckill.vo.SeckillSessionWithSkus;

import java.util.List;

/**
 * 秒杀服务
 *
 * @author miozus
 * @date 2022/03/07
 */
public interface SeckillService {
    /**
     * 上传continuous3天sku
     */
    void uploadContinuous3DaySku();

    /**
     * 保存秒杀商品会话数据 Redis
     *
     * @param sessionData 会话数据
     */
    void saveSessionDataRedis(List<SeckillSessionWithSkus> sessionData);

    /**
     * 获取秒杀sku信息
     *
     * @return {@link SeckillSkuRedisTo}
     */
    List<SeckillSkuRedisTo> fetchSeckillSkuInfo();
}
