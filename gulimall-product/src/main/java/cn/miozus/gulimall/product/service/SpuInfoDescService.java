package cn.miozus.gulimall.product.service;

import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.product.entity.SpuInfoDescEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * spu信息介绍
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-06 23:57:18
 */
public interface SpuInfoDescService extends IService<SpuInfoDescEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfoDesc(SpuInfoDescEntity descEntity);

}

