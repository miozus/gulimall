package cn.miozus.gulimall.ware.feign;


import cn.miozus.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 调用成员服务
 *
 * @author miao
 * @date 2022/01/15
 */
@FeignClient("gulimall-member")
public interface MemberFeignService {

    @RequestMapping("/member/memberreceiveaddress/info/{id}")
    public R info(@PathVariable("id") Long id);

}
