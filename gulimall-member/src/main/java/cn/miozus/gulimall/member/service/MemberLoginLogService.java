package cn.miozus.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.member.entity.MemberLoginLogEntity;

import java.util.Map;

/**
 * 会员登录记录
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:13:14
 */
public interface MemberLoginLogService extends IService<MemberLoginLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

