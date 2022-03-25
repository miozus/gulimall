package cn.miozus.gulimall.seckill.to;

import cn.hutool.core.date.DateUtil;
import cn.miozus.gulimall.seckill.vo.SkuInfoVo;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 秒杀sku Redis 传输对象
 *
 * @author miozus
 * @date 2022/03/08
 */
@Data
public class SeckillSkuRedisTo {

    /**
     * id
     */
    private Long id;
    /**
     * 活动id
     */
    private Long promotionId;
    /**
     * 活动场次id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 秒杀总量
     */
    private Integer seckillCount;
    /**
     * 每人限购数量
     */
    private Integer seckillLimit;
    /**
     * 排序
     */
    private Integer seckillSort;

    /**
     * 每日开始时间
     */
    private Long startTime;
    /**
     * 每日结束时间
     */
    private Long endTime;

    /** 开抢携带随机码  */
    private String randomCode;

    /** 是系统时间处在活动时间内 */
    public boolean isExpiryDate() {
        long now = DateUtil.current();
        return (this.startTime <= now && now <= this.endTime);
    }
    public boolean isNotExpiryDate() {
        return !isExpiryDate();
    }

    /** 获取秒杀活动剩余时间 */
    public Long getTTL(){
        long now = DateUtil.current();
        return this.endTime - now;
    }

    /** sku 详情 */
    private SkuInfoVo skuInfo;

}
