package cn.miozus.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.product.entity.CommentReplayEntity;

import java.util.Map;

/**
 * 商品评价回复关系
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-06 23:57:18
 */
public interface CommentReplayService extends IService<CommentReplayEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

