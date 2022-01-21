package cn.miozus.gulimall.ware.service.impl;

import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.common.utils.R;
import cn.miozus.gulimall.ware.dao.WareInfoDao;
import cn.miozus.gulimall.ware.entity.WareInfoEntity;
import cn.miozus.gulimall.ware.feign.MemberFeignService;
import cn.miozus.gulimall.ware.service.WareInfoService;
import cn.miozus.gulimall.ware.vo.FareVo;
import cn.miozus.gulimall.ware.vo.MemberReceiveAddressVo;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    MemberFeignService memberService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                new QueryWrapper<WareInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryWareInfoPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        QueryWrapper<WareInfoEntity> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(key)) {
            wrapper.and(obj ->
                    obj.eq("id", key)
                            .or()
                            .like("name", key)
                            .or()
                            .like("address", key)
                            .or()
                            .like("areacode", key)
            );
        }
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }

    /**
     * 虚假的查询运费：电话号码截取最后两位
     * 真实场景：快递100接口 = 地点 + 货物重量/类别 + 不同快递公司报价
     *
     * @param addrId addr id
     * @return {@link BigDecimal}
     */
    @Override
    public FareVo queryFare(Long addrId) {
        R info = memberService.info(addrId);
        if (info.getCode() != 0) {
            return null;
        }
        MemberReceiveAddressVo data = info.getData("memberReceiveAddress", new TypeReference<MemberReceiveAddressVo>() {
        });
        FareVo fareVo = new FareVo();
        String phone = data.getPhone();
        String substring = phone.substring(phone.length() - 1);
        BigDecimal fare = new BigDecimal(substring);
        fareVo.setAddress(data);
        fareVo.setFare(fare);

        return fareVo;
    }

}