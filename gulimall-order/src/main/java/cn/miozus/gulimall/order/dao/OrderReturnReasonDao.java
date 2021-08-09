package cn.miozus.gulimall.order.dao;

import cn.miozus.gulimall.order.entity.OrderReturnReasonEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退货原因
 * 
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:18:03
 */
@Mapper
public interface OrderReturnReasonDao extends BaseMapper<OrderReturnReasonEntity> {
	
}
