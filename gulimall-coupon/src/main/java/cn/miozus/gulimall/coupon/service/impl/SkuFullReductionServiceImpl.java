package cn.miozus.gulimall.coupon.service.impl;

import cn.miozus.gulimall.common.to.MemberPrice;
import cn.miozus.gulimall.common.to.SkuReductionTo;
import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.common.utils.Query;
import cn.miozus.gulimall.coupon.dao.SkuFullReductionDao;
import cn.miozus.gulimall.coupon.entity.MemberPriceEntity;
import cn.miozus.gulimall.coupon.entity.SkuFullReductionEntity;
import cn.miozus.gulimall.coupon.entity.SkuLadderEntity;
import cn.miozus.gulimall.coupon.service.MemberPriceService;
import cn.miozus.gulimall.coupon.service.SkuFullReductionService;
import cn.miozus.gulimall.coupon.service.SkuLadderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {

    @Autowired
    SkuLadderService skuLadderService;

    @Autowired
    MemberPriceService memberPriceService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<SkuFullReductionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuReduction(SkuReductionTo skuReductionTo) {
        // 保存满减打折，会员价
        // sms_sku_ladder（打折）,sms_sku_full_reduction（满减）,sms_member_price（会员价）

        //  sms_sku_ladder（打折）
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        skuLadderEntity.setSkuId(skuReductionTo.getSkuId());
        skuLadderEntity.setFullCount(skuReductionTo.getFullCount());
        skuLadderEntity.setDiscount(skuReductionTo.getDiscount());
        skuLadderEntity.setAddOther(skuReductionTo.getCountStatus());
        //skuLadderEntity.setPrice(); // 最后提交订单算折扣价，或者sku加进来算折扣
        if (skuReductionTo.getFullCount() > 0) {
            skuLadderService.save(skuLadderEntity);
        }

        // sms_sku_full_reduction（满减）
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuReductionTo, skuFullReductionEntity);
        skuFullReductionEntity.setAddOther(skuReductionTo.getCountStatus());
        if (skuFullReductionEntity.getFullPrice().compareTo(BigDecimal.ZERO) > 0) {
            this.save(skuFullReductionEntity);
        }

        // sms_member_price（会员价）
        List<MemberPrice> memberPrice = skuReductionTo.getMemberPrice();
        List<MemberPriceEntity> collect = memberPrice.stream().map(price -> {
                    MemberPriceEntity memberPriceEntity = new MemberPriceEntity();
                    memberPriceEntity.setSkuId(skuReductionTo.getSkuId());
                    memberPriceEntity.setMemberLevelId(price.getId());
                    memberPriceEntity.setMemberPrice(price.getPrice());
                    memberPriceEntity.setMemberLevelName(price.getName());
                    memberPriceEntity.setAddOther(skuReductionTo.getCountStatus());
                    return memberPriceEntity;
                }).filter(price -> price.getMemberPrice().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        memberPriceService.saveBatch(collect);
    }

}