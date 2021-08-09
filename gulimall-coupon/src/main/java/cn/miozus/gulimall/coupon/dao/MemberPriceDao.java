package cn.miozus.gulimall.coupon.dao;

import cn.miozus.gulimall.coupon.entity.MemberPriceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品会员价格
 * 
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-07 16:30:51
 */
@Mapper
public interface MemberPriceDao extends BaseMapper<MemberPriceEntity> {
	
}
