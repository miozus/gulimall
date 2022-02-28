package cn.miozus.gulimall.ware.service;

import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.ware.entity.PurchaseEntity;
import cn.miozus.gulimall.ware.vo.MergeVo;
import cn.miozus.gulimall.ware.vo.PurchaseDoneVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 采购信息
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:20:54
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryUnreceiveListPage(Map<String, Object> params);

    void mergePurchase(MergeVo mergeVo);

    void receivePurchase(List<Long> ids);

    void done(PurchaseDoneVo doneVo);
}

