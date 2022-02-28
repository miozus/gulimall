package cn.miozus.gulimall.member.service.impl;

import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.member.feign.OrderFeignService;
import cn.miozus.gulimall.member.service.MemberWebService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service("memberWebService")
public class MemberWebServiceImpl implements MemberWebService {

    @Autowired
    OrderFeignService orderFeignService;

    @Override
    public R renderPage(String pageNum) {
        HashMap<String, Object> params = new HashMap<>(1);
        params.put("page", pageNum);
        return orderFeignService.listWithItems(params);
    }

}
