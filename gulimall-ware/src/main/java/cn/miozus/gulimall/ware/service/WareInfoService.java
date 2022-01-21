package cn.miozus.gulimall.ware.service;

import cn.miozus.gulimall.ware.vo.FareVo;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.gulimall.ware.entity.WareInfoEntity;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 仓库信息
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:20:54
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryWareInfoPage(Map<String, Object> params);

    /**
     * 查询运费
     *
     * @param addrId addr id
     * @return {@link BigDecimal}
     */
    FareVo queryFare(Long addrId);
}

