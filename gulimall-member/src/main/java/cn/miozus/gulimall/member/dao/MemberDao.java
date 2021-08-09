package cn.miozus.gulimall.member.dao;

import cn.miozus.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:13:14
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
