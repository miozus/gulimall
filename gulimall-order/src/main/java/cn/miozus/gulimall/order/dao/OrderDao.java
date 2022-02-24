package cn.miozus.gulimall.order.dao;

import cn.miozus.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单
 * 
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:18:03
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {

    /**
     * 更新订单状态
     *
     * @param outTradeNo 订单号
     * @param code       代码
     */
    void updateOrderStatus(@Param("outTradeNo") String outTradeNo, @Param("code") int code);
}
