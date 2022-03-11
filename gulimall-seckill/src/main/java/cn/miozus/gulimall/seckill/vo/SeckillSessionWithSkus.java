package cn.miozus.gulimall.seckill.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 秒杀会话sku
 *
 * @author miozus
 * @date 2022/03/08
 */
@Data
public class SeckillSessionWithSkus {

    /**
     * id
     */
    private Long id;
    /**
     * 场次名称
     */
    private String name;
    /**
     * 每日开始时间
     */
    private Date startTime;
    /**
     * 每日结束时间
     */
    private Date endTime;
    /**
     * 启用状态
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * sku秒杀关联商品
     */
    @TableField(exist = false)
    private List<SeckillSkuVo> relationSkus;
}
