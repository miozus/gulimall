package cn.miozus.gulimall.member.web;

import cn.miozus.common.utils.R;
import cn.miozus.gulimall.member.feign.OrderFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;

/**
 * 会员服务网页控制台
 *
 * @author miao
 * @date 2022/01/26
 */
@Slf4j
@Controller
public class MemberWebController {
    @Autowired
    OrderFeignService orderFeignService;

    /**
     * 会员订单页面，查询第 N 页数据
     *
     * @param pageNum 页面num
     * @return {@link String}
     */
    @GetMapping("/memberOrder.html")
    public String memberOrderPage(@RequestParam(value="pageNum", defaultValue="1") String pageNum, Model model) {
        HashMap<String, Object> params = new HashMap<>(1);
        params.put("page", pageNum);
        R r = orderFeignService.listWithItems(params);
        log.debug("📒 r {} ", r);
        model.addAttribute("orders", r);
        return "orderList";
    }

}
