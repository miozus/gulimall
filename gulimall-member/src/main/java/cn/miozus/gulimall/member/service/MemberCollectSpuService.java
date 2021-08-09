package cn.miozus.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.gulimall.member.entity.MemberCollectSpuEntity;

import java.util.Map;

/**
 * 会员收藏的商品
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:13:14
 */
public interface MemberCollectSpuService extends IService<MemberCollectSpuEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

