package cn.miozus.gulimall.coupon.dao;

import cn.miozus.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-07 16:30:51
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
