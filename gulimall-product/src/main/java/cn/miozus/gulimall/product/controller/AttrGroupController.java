package cn.miozus.gulimall.product.controller;

import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.R;
import cn.miozus.gulimall.product.entity.AttrEntity;
import cn.miozus.gulimall.product.entity.AttrGroupEntity;
import cn.miozus.gulimall.product.service.AttrAttrgroupRelationService;
import cn.miozus.gulimall.product.service.AttrGroupService;
import cn.miozus.gulimall.product.service.AttrService;
import cn.miozus.gulimall.product.service.CategoryService;
import cn.miozus.gulimall.product.vo.AttrGroupRelationVo;
import cn.miozus.gulimall.product.vo.AttrGroupWithAttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * 属性分组
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-06 23:57:18
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    AttrService attrService;

    @Autowired
    AttrAttrgroupRelationService relationService;


    /**
     * 列表
     */
    @RequestMapping("/list/{catelogId}")
    public R list(@RequestParam Map<String, Object> params, @PathVariable("catelogId") Long catelogId) {
        //PageUtils page = attrGroupService.queryPage(params);
        PageUtils page = attrGroupService.queryPage(params, catelogId);

        return R.ok().put("page", page);
    }

    /**
     * attr关系
     *
     * @param attrgroupId attrgroup id
     * @return {@link R}
     */
    @GetMapping("/{attrgroupId}/attr/relation")
    public R relationAttr(@PathVariable("attrgroupId") Long attrgroupId) {
        List<AttrEntity> attrEntities = attrService.getRelationAttr(attrgroupId);
        return R.ok().put("data", attrEntities);
    }

    @GetMapping("/{attrgroupId}/noattr/relation")
    public R noRelationAttr(@RequestParam Map<String, Object> params, @PathVariable("attrgroupId") Long attrgroupId) {
        PageUtils page = attrService.getNoRelationAttr(params, attrgroupId);
        return R.ok().put("page", page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    public R info(@PathVariable("attrGroupId") Long attrGroupId) {
        AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);
        Long catelogId = attrGroup.getCatelogId();
        // 子目录完整路径：[2, 34, 225]
        Long[] path = categoryService.findCatelogPath(catelogId);
        attrGroup.setCatelogPath(path);

        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * attr group with attrs
     *
     * @param catelogId catelogId
     * @return {@link R}
     * @see R
     */
    @GetMapping("/{catelogId}/withattr")
    public R attrGroupWithAttrs(@PathVariable("catelogId") Long catelogId) {
        // 所有属性分组
        // 每个属性分组之下的所有属性（嵌套）
        List<AttrGroupWithAttrVo> vos = attrGroupService.getAttrGroupWithAttrsByCatelogId(catelogId);

        return R.ok().put("data", vos);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 添加关系
     *
     * @param vos List<AttrGroupRelationVo>
     * @return {@link R}
     */
    @PostMapping("/attr/relation")
    public R addRelation(@RequestBody List<AttrGroupRelationVo> vos) {
        relationService.addRelationBatch(vos);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrGroupIds) {
        attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

    @PostMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody AttrGroupRelationVo[] vos) {
        attrGroupService.removeRelation(vos);

        return R.ok();
    }

}
