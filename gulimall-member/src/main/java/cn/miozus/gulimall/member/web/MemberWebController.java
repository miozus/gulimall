package cn.miozus.gulimall.member.web;

import cn.miozus.gulimall.common.enume.SysLog;
import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.member.service.MemberWebService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    MemberWebService memberWebService;

    /**
     * 会员订单页面，查询第 N 页数据
     *
     * @param pageNum 页面num
     * @return {@link String}
     */
    @GetMapping("/memberOrder.html")
    @SysLog("获取购物车列表")
    public String memberOrderPage(@RequestParam(value = "pageNum", defaultValue = "1") String pageNum, Model model) {
        R r = memberWebService.renderPage(pageNum);
        log.debug("📒 r {} ", r);
        model.addAttribute("orders", r);
        return "orderList";
    }


}
