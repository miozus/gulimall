package cn.miozus.gulimall.order.vo;

import com.alibaba.nacos.common.utils.CollectionUtils;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单确认签证官
 *
 * @author miao
 * @date 2022/01/15
 */
@Data
public class OrderConfirmVo {
    /** 收货人信息 */
    private List<MemberReceiveAddressVo> address;
    /** 单品 */
    private List<OrderItemVo> items;
    /** 发票 */

    Map<Long, Boolean> stocks;
    /** 积分 */
    private Integer integration;
    /** 商品数量 */
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private Integer count;
    /** 商品总价 */
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private BigDecimal total;
    /** 应付金额 */
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private BigDecimal payPrice;


    /** 运费 */
    private BigDecimal fare;
    /** 防重令牌 */
    private String orderToken;


    /**
     * 总金额 = 商品总价 - 减免优惠
     *
     * 勾选：才计算
     *
     * @return {@link BigDecimal} 超级静态的变量：调用者不会改变，必须找到接收者
     */
    public BigDecimal getTotal() {
        BigDecimal amount = BigDecimal.ZERO;
        if (CollectionUtils.isNotEmpty(items)) {
            for (OrderItemVo item : items) {
                BigDecimal count = new BigDecimal(item.getCount().toString());
                BigDecimal multiply = item.getPrice().multiply(count);
                    amount = amount.add(multiply);
            }
        }
        return amount;
    }

    /**
     * 数量小计
     *
     * @return {@link Integer}
     */
    public Integer getCount() {
        int count = 0;
        if (CollectionUtils.isNotEmpty(items)) {
            for (OrderItemVo item : items) {
                count += item.getCount();
            }
        }
        return count;
    }

    /**
     * 获得支付价格
     * [TODO] 用优惠券数据计算
     *
     * @return {@link BigDecimal}
     */
    public BigDecimal getPayPrice() {
        return getTotal();
    }

}
