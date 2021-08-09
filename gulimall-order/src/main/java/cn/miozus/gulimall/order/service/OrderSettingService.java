package cn.miozus.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.gulimall.order.entity.OrderSettingEntity;

import java.util.Map;

/**
 * 订单配置信息
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:18:03
 */
public interface OrderSettingService extends IService<OrderSettingEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

