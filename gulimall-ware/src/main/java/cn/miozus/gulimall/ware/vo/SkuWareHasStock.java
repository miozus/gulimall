package cn.miozus.gulimall.ware.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * sku涉及到的所有仓库
 *
 * @author miao
 * @date 2022/01/22
 */
@Data
@Builder
public class SkuWareHasStock {
    private Long skuId;
    private List<Long> wareIds;
    /** 购买数量 */
    private Integer skuNum;
    private String skuName;
}
