package cn.miozus.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单项签证官
 *
 * @author miao
 * @date 2022/01/15
 */
@Data
public class OrderItemVo {
    private Long skuId;
    private String title ;
    private String image ;
    private List<String> skuAttrs;
    private BigDecimal price;
    private Integer count;
    private BigDecimal totalPrice;

    /** 查表 */
    private BigDecimal weight;


}
