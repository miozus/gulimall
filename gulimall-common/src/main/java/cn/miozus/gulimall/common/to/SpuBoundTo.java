package cn.miozus.gulimall.common.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * spu绑定到
 *
 * @author miao
 * @date 2021/09/21
 */// pms -> sms 都要使用的对象
@Data
public class SpuBoundTo {

    private Long spuId;
    private BigDecimal buyBounds;
    private BigDecimal growBounds;
}
