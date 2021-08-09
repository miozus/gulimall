package cn.miozus.gulimall.coupon.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.miozus.gulimall.coupon.entity.CouponEntity;
import cn.miozus.gulimall.coupon.service.CouponService;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.R;


/**
 * 优惠券信息
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-07 16:30:51
 */
@RefreshScope
@RestController
@RequestMapping("coupon/coupon")
public class CouponController {
    @Autowired
    private CouponService couponService;

    @Value("${coupon.user.name}")
    public String name;
    @Value("${coupon.user.age}")
    public Integer age;
    @Value("${coupon.user.sex}")
    public String sex;
    @Value("${coupon.user.email}")
    public String email;

    @RequestMapping("config")
    public R testConfig() {
        return R.ok().put("name", name).put("age", age).put("sex", sex).put("email", email);
    }

    /**
     * 测试调用优惠券服务
     */
    @RequestMapping("/member/list")
    public R memberCoupon() {
        CouponEntity couponEntity = new CouponEntity();
        couponEntity.setCouponName("满90减30");
        return R.ok().put("coupons", Arrays.asList(couponEntity));

    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = couponService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        CouponEntity coupon = couponService.getById(id);

        return R.ok().put("coupon", coupon);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody CouponEntity coupon) {
        couponService.save(coupon);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody CouponEntity coupon) {
        couponService.updateById(coupon);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        couponService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
