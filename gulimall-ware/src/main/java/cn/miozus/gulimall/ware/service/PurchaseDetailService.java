package cn.miozus.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.gulimall.ware.entity.PurchaseDetailEntity;

import java.util.Map;

/**
 * 
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:20:54
 */
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

