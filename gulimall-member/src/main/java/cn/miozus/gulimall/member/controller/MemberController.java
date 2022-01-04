package cn.miozus.gulimall.member.controller;

import cn.miozus.common.exception.BizCodeEnum;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.R;
import cn.miozus.gulimall.member.entity.MemberEntity;
import cn.miozus.gulimall.member.exception.PhoneNumberAlreadyExistsException;
import cn.miozus.gulimall.member.exception.UsernameAlreadyExistsException;
import cn.miozus.gulimall.member.feign.CouponFeignService;
import cn.miozus.gulimall.member.service.MemberService;
import cn.miozus.gulimall.member.vo.MemberLoginVo;
import cn.miozus.gulimall.member.vo.MemberRegisterVo;
import cn.miozus.gulimall.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;


/**
 * 会员
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:13:14
 */
@RestController
@RequestMapping("/member/member")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @Autowired
    CouponFeignService couponFeignService;

    @RequestMapping("/coupons")
    public R test() {
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("海贼王");

        R memberCoupon = couponFeignService.memberCoupons();
        // 本地查询到的 + 远程调用的
        return R.ok().put("members", memberEntity).put("coupons", memberCoupon.get("coupons"));
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody MemberEntity member) {
        memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody MemberEntity member) {
        memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    @PostMapping("/register")
    public R register(@RequestBody MemberRegisterVo vo) {
        try {
            memberService.register(vo);
        } catch (UsernameAlreadyExistsException e) {
            return R.error(BizCodeEnum.USERNAME_ALREADY_EXISTS_EXCEPTION.getCode(),
                    BizCodeEnum.USERNAME_ALREADY_EXISTS_EXCEPTION.getMsg());
        } catch (PhoneNumberAlreadyExistsException e) {
            return R.error(BizCodeEnum.PHONE_ALREADY_EXISTS_EXCEPTION.getCode(),
                    BizCodeEnum.PHONE_ALREADY_EXISTS_EXCEPTION.getMsg());
        }
        return R.ok();
    }

    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo vo) {
        MemberEntity member = memberService.login(vo);
        if (member == null) {
            return R.error(BizCodeEnum.USERNAME_OR_PASSWORD_INVALID_EXCEPTION.getCode(),
                    BizCodeEnum.USERNAME_OR_PASSWORD_INVALID_EXCEPTION.getMsg());
        }
        return R.ok().setData(member);
    }


    @PostMapping("/oauth2/login")
    public R oauthLogin(@RequestBody SocialUser socialUser) {
        MemberEntity member = memberService.login(socialUser);
        if (member == null) {
            return R.error(BizCodeEnum.USERNAME_OR_PASSWORD_INVALID_EXCEPTION.getCode(),
                    BizCodeEnum.USERNAME_OR_PASSWORD_INVALID_EXCEPTION.getMsg());
        }
        return R.ok().setData(member);
    }

}
