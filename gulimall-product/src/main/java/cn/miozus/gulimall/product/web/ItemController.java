package cn.miozus.gulimall.product.web;

import cn.miozus.gulimall.product.service.SkuInfoService;
import cn.miozus.gulimall.product.vo.SkuItemVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品详情页控制器
 *
 * @author miao
 * @date 2021/12/24
 */
@Controller
@Slf4j
public class ItemController {

    @Autowired
    SkuInfoService skuInfoService;

    /**
     * 展示详情 sku
     *
     * @return {@link String}
     * @see String
     */
    @GetMapping("/{skuId}.html")
    public String querySkuItem(@PathVariable("skuId") Long skuId, Model model) {
        log.info("准备查询商品详情[skuId:{}]", skuId);
        SkuItemVo vo = skuInfoService.querySkuItem(skuId);
        model.addAttribute("item", vo);
        return "item";
    }

}
