package cn.miozus.gulimall.ware.service.impl;

import cn.miozus.gulimall.common.constant.WareConstant;
import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.common.utils.Query;
import cn.miozus.gulimall.ware.dao.PurchaseDao;
import cn.miozus.gulimall.ware.entity.PurchaseDetailEntity;
import cn.miozus.gulimall.ware.entity.PurchaseEntity;
import cn.miozus.gulimall.ware.service.PurchaseDetailService;
import cn.miozus.gulimall.ware.service.PurchaseService;
import cn.miozus.gulimall.ware.service.WareSkuService;
import cn.miozus.gulimall.ware.vo.MergeVo;
import cn.miozus.gulimall.ware.vo.PurchaseDoneItemVo;
import cn.miozus.gulimall.ware.vo.PurchaseDoneVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    PurchaseDetailService purchaseDetailService;

    @Autowired
    WareSkuService wareskuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryUnreceiveListPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
                        .eq("status", WareConstant.PurchaseStatusEnum.CREATED.getCode())
                        .or()
                        .eq("status", WareConstant.PurchaseStatusEnum.RECEIVED.getCode())
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        if (purchaseId == null) {
            // ??????????????????, ?????? id ??????????????????
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }
        // ???????????????????????????????????????,purchase_id and status
        Long finalPurchaseId = purchaseId;
        List<Long> items = mergeVo.getItems();
        List<PurchaseDetailEntity> detailEntities = purchaseDetailService.listByIds(items);
        List<PurchaseDetailEntity> collect = detailEntities.stream()
                // purchase_detail ????????????????????????????????????????????????????????????
                .filter(entity -> entity.getStatus() == WareConstant.PurchaseDetailStatusEnum.CREATED.getCode() ||
                        entity.getStatus() == WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode())
                .map(entity -> {
                    PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
                    purchaseDetailEntity.setId(entity.getId());
                    // ??????????????????????????????
                    purchaseDetailEntity.setPurchaseId(finalPurchaseId);
                    purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
                    return purchaseDetailEntity;
                }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(collect);
        // ????????????????????????????????????
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

    /**
     * ????????????
     *
     * @param ids id
     */
    @Transactional
    @Override
    public void receivePurchase(List<Long> ids) {
        // purchase ?????????: ??????????????????????????????????????????
        List<PurchaseEntity> collect = ids.stream()
                // ???????????? map(id -> this.getById(id))
                .map(this::getById)
                .filter(entity -> entity.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() ||
                        entity.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()
                ).map(entity -> {
                    // purchase ?????????: ????????????
                    entity.setStatus(WareConstant.PurchaseStatusEnum.RECEIVED.getCode());
                    entity.setUpdateTime(new Date());
                    return entity;
                }).collect(Collectors.toList());
        this.updateBatchById(collect);
        // purchase_detail ????????????????????????????????????
        collect.forEach(purchaseEntity -> {
            List<PurchaseDetailEntity> entities = purchaseDetailService.listDetailByPurchaseId(purchaseEntity.getId());
            List<PurchaseDetailEntity> detailEntities = entities.stream().map(entity -> {
                PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
                detailEntity.setId(entity.getId());
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                return detailEntity;
            }).collect(Collectors.toList());
            purchaseDetailService.updateBatchById(detailEntities);
        });
    }

    @Transactional
    @Override
    public void done(PurchaseDoneVo doneVo) {
        // purchase ?????????????????????
        Long id = doneVo.getId();
        // purchase_detail ?????????????????????
        boolean isSuccess = true;
        List<PurchaseDoneItemVo> items = doneVo.getItems();
        List<PurchaseDetailEntity> updates = new ArrayList<>();
        for (PurchaseDoneItemVo item : items) {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            if (item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()) {
                isSuccess = false;
                detailEntity.setStatus(item.getStatus());
            } else {
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISHED.getCode());
                // ware_sku ???????????????????????????????????????????????????
                PurchaseDetailEntity entity = purchaseDetailService.getById(item.getItemId());
                wareskuService.addStock(
                        entity.getSkuId(), entity.getWareId(), entity.getSkuNum()
                );
            }
            detailEntity.setId(item.getItemId());
            updates.add(detailEntity);
        }
        purchaseDetailService.updateBatchById(updates);
        // purchase ?????????????????????
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(id);
        purchaseEntity.setStatus(Boolean.TRUE.equals(isSuccess) ?
                WareConstant.PurchaseStatusEnum.FINISHED.getCode() :
                WareConstant.PurchaseStatusEnum.HASERROR.getCode()
        );
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

}