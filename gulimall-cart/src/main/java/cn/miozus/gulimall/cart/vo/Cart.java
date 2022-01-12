package cn.miozus.gulimall.cart.vo;

import com.alibaba.nacos.common.utils.CollectionUtils;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车数据
 * <p>
 * 需要计算的属性：必须重写它的 get 方法，保证每次都会计算
 *
 * @author miao
 * @date 2022/01/04
 */
@ToString
public class Cart {
    List<CartItem> items;
    /**
     * 商品数量
     */
    private Integer countNum;
    /**
     * 商品类型数量
     */
    private Integer countType;

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * 商品总价
     */
    private BigDecimal totalAmount;
    /**
     * 优惠减免价格
     */
    private BigDecimal reduce = BigDecimal.ZERO;


    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    /**
     * 得到数num
     *
     * @return {@link Integer}
     */
    public Integer getCountNum() {
        int count = 0;
        if (CollectionUtils.isNotEmpty(items)) {
            for (CartItem item : items) {
                count += item.getCount();
            }
        }
        return count;
    }

    public Integer getCountType() {
        int count = 0;
        if (CollectionUtils.isNotEmpty(items)) {
            for (CartItem item : items) {
                count += 1;
            }
        }
        return count;
    }

    /**
     * 总金额 = 商品总价 - 减免优惠
     *
     * 勾选：才计算
     *
     * @return {@link BigDecimal} 超级静态的变量：调用者不会改变，必须找到接收者
     */
    public BigDecimal getTotalAmount() {
        BigDecimal amount = BigDecimal.ZERO;
        if (CollectionUtils.isNotEmpty(items)) {
            for (CartItem item : items) {
                if (item.getIsChecked()) {
                    amount = amount.add(item.getTotalPrice());
                }
            }
        }
        return amount.subtract(reduce);
    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
