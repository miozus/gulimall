package cn.miozus.gulimall.order.feign;

import cn.miozus.gulimall.order.vo.MemberReceiveAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 调用会员服务
 *
 * @author miao
 * @date 2022/01/15
 */
@FeignClient("gulimall-member")
public interface MemberFeignService {

    @GetMapping("/member/memberreceiveaddress/{memberId}/address")
    public List<MemberReceiveAddressVo> queryAddressByMemberId(@RequestParam("memberId") Long memberId);

}
