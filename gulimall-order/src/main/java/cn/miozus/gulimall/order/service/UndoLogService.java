package cn.miozus.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.gulimall.order.entity.UndoLogEntity;

import java.util.Map;

/**
 * 
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:18:03
 */
public interface UndoLogService extends IService<UndoLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

