package cn.miozus.gulimall.product.web;

import cn.miozus.gulimall.product.service.SkuInfoService;
import cn.miozus.gulimall.product.vo.SkuItemVo;
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
    public String skuItem(@PathVariable("skuId") Long skuId, Model model) {
        System.out.println("准备查询" + skuId + "详情");
        SkuItemVo vo =  skuInfoService.item(skuId);
        model.addAttribute("item", vo);
        return "item";
    }

}
