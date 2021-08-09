package cn.miozus.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.gulimall.coupon.entity.SkuLadderEntity;

import java.util.Map;

/**
 * 商品阶梯价格
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-07 16:30:51
 */
public interface SkuLadderService extends IService<SkuLadderEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

