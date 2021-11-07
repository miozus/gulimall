package cn.miozus.gulimall.search.controller;

import cn.miozus.gulimall.search.service.MallSearchService;
import cn.miozus.gulimall.search.vo.SearchParam;
import cn.miozus.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 搜索控制器
 *
 * @author miao
 * @date 2021/10/23
 */
@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    /**
     * 自动将页面提交过来的所有请求查询参数，封装成指定对象
     * @param param 参数
     * @return {@link String}
     */
    @GetMapping({ "/list.html", "/search.html" })
    public String listPage(SearchParam param, Model model) {
        // 根据传递来的参数，去 ES 中检索商品
        SearchResult result = mallSearchService.search(param);
        model.addAttribute("result", result);
        return "search";
    }

}
