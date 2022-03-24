package cn.miozus.gulimall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 秒杀sku缓存来
 *
 * @author Miozus
 * @date 2022/03/24
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


}
