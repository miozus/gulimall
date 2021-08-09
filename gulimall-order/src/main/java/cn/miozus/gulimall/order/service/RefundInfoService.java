package cn.miozus.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.gulimall.order.entity.RefundInfoEntity;

import java.util.Map;

/**
 * 退款信息
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:18:03
 */
public interface RefundInfoService extends IService<RefundInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

