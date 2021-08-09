package cn.miozus.gulimall.ware.dao;

import cn.miozus.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品库存
 * 
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:20:54
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {
	
}
