package cn.miozus.gulimall.ware.service.impl;

import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.gulimall.ware.dao.PurchaseDetailDao;
import cn.miozus.gulimall.ware.entity.PurchaseDetailEntity;
import cn.miozus.gulimall.ware.service.PurchaseDetailService;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service("purchaseDetailService")
public class PurchaseDetailServiceImpl extends ServiceImpl<PurchaseDetailDao, PurchaseDetailEntity> implements PurchaseDetailService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseDetailEntity> page = this.page(
                new Query<PurchaseDetailEntity>().getPage(params),
                new QueryWrapper<PurchaseDetailEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPurchaseDetialPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        QueryWrapper<PurchaseDetailEntity> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(key)) {
            wrapper.and(obj ->
                    obj.eq("purchase_id", key)
                            .or()
                            .eq("sku_id", key)
            );
        }
        String wareId = (String) params.get("wareId");
        if (StringUtils.isNotBlank(wareId)) {
            wrapper.eq("ware_id", wareId);
        }
        String status = (String) params.get("status");
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq("status", status);
        }
        IPage<PurchaseDetailEntity> page = this.page(
                new Query<PurchaseDetailEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }

    @Override
    public List<PurchaseDetailEntity> listDetailByPurchaseId(Long id) {
        return this.list(new QueryWrapper<PurchaseDetailEntity>().eq("purchase_id", id));
    }

}