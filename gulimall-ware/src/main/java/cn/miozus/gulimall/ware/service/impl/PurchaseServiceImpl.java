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
            // 新建一条记录, 生成 id 赋值到条件中
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }
        // 修改采购项目的状态（合并）,purchase_id and status
        Long finalPurchaseId = purchaseId;
        List<Long> items = mergeVo.getItems();
        List<PurchaseDetailEntity> detailEntities = purchaseDetailService.listByIds(items);
        List<PurchaseDetailEntity> collect = detailEntities.stream()
                // purchase_detail 确认采购需求状态是新建或已分配，才能合并
                .filter(entity -> entity.getStatus() == WareConstant.PurchaseDetailStatusEnum.CREATED.getCode() ||
                        entity.getStatus() == WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode())
                .map(entity -> {
                    PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
                    purchaseDetailEntity.setId(entity.getId());
                    // 合并到同一个采购单号
                    purchaseDetailEntity.setPurchaseId(finalPurchaseId);
                    purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
                    return purchaseDetailEntity;
                }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(collect);
        // 采购单一变，更新日期也变
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

    /**
     * 接受购买
     *
     * @param ids id
     */
    @Transactional
    @Override
    public void receivePurchase(List<Long> ids) {
        // purchase 采购单: 确认新建或已分配（保险起见）
        List<PurchaseEntity> collect = ids.stream()
                // 超级简拼 map(id -> this.getById(id))
                .map(this::getById)
                .filter(entity -> entity.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() ||
                        entity.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()
                ).map(entity -> {
                    // purchase 采购单: 更新状态
                    entity.setStatus(WareConstant.PurchaseStatusEnum.RECEIVED.getCode());
                    entity.setUpdateTime(new Date());
                    return entity;
                }).collect(Collectors.toList());
        this.updateBatchById(collect);
        // purchase_detail 更新采购需求（项目）状态
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
        // purchase 更新采购单状态
        Long id = doneVo.getId();
        // purchase_detail 更新采购项状态
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
                // ware_sku 将成功采购的进行入库（数量了增加）
                PurchaseDetailEntity entity = purchaseDetailService.getById(item.getItemId());
                wareskuService.addStock(
                        entity.getSkuId(), entity.getWareId(), entity.getSkuNum()
                );
            }
            detailEntity.setId(item.getItemId());
            updates.add(detailEntity);
        }
        purchaseDetailService.updateBatchById(updates);
        // purchase 更新采购单状态
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