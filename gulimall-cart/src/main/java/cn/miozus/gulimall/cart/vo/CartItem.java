package cn.miozus.gulimall.cart.vo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车商品
 *
 * @author miao
 * @date 2022/01/04
 */
@Data
public class CartItem {
    private Long skuId;

    /**
     * 选中与否
     */
    private Boolean isChecked = true;
    private String title ;
    private String image ;
    private List<String> skuAttrs;
    private BigDecimal price;
    private Integer count;
    @Setter(AccessLevel.NONE)
    private BigDecimal totalPrice;
    /**
     * 得到总价格
     *
     * @return {@link BigDecimal}
     */
    public BigDecimal getTotalPrice() {
        return this.price.multiply(new BigDecimal("" + this.count));
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
