package cn.miozus.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.order.entity.PaymentInfoEntity;

import java.util.Map;

/**
 * 支付信息表
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:18:03
 */
public interface PaymentInfoService extends IService<PaymentInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

