package cn.miozus.gulimall.product.controller;

import java.util.Arrays;
import java.util.Map;

import cn.miozus.gulimall.product.vo.AttrRespVo;
import cn.miozus.gulimall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import cn.miozus.gulimall.product.service.AttrService;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.R;


/**
 * 商品属性
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-06 23:57:18
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 列表
     */
    @GetMapping("/base/list/{catlogId}")
    public R listBaseAttr(@RequestParam Map<String, Object> params, @PathVariable("catlogId") Long catlogId) {
        PageUtils page = attrService.queryBaseAttrPage(params, catlogId);

        return R.ok().put("page", page);
    }


    @RequestMapping("/info/{attrId}")
    public R info(@PathVariable("attrId") Long attrId) {
        AttrRespVo attr = attrService.getAttrById(attrId);

        return R.ok().put("attr", attr);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody /*AttrEntity*/ AttrVo attr) {
        attrService.saveAttr(attr);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrVo attr) {
        attrService.updateAttr(attr);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrIds) {
        attrService.removeByIds(Arrays.asList(attrIds));

        return R.ok();
    }

}
