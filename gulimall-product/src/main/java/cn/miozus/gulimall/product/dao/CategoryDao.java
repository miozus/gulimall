package cn.miozus.gulimall.product.dao;

import cn.miozus.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-06 23:57:18
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
