package cn.miozus.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.coupon.entity.UndoLogEntity;

import java.util.Map;

/**
 * 
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-07 16:30:51
 */
public interface UndoLogService extends IService<UndoLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

