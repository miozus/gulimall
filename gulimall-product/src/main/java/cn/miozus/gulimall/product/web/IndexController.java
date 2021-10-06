package cn.miozus.gulimall.product.web;

import cn.miozus.gulimall.product.entity.CategoryEntity;
import cn.miozus.gulimall.product.service.CategoryService;
import cn.miozus.gulimall.product.vo.Catalog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * 指数控制器
 *
 * @author miao
 * @date 2021/10/04
 */
@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;


    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model) {
        // 查询一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categories();
        model.addAttribute("categories", categoryEntities);
        return "index";
    }

    /**
     * 获取目录 JSON
     *
     * @return {@link Map}<{@link Integer}, {@link Object}> 适用[JSON]
     * @Annotation ResponseBody 以JSON 格式返回
     */
    @ResponseBody
    @GetMapping("/index/json/catalog.json")
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        return categoryService.getCatalogJson();
    }
}