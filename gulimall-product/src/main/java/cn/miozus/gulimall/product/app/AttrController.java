package cn.miozus.gulimall.product.app;

import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.R;
import cn.miozus.gulimall.product.entity.ProductAttrValueEntity;
import cn.miozus.gulimall.product.service.AttrService;
import cn.miozus.gulimall.product.service.ProductAttrValueService;
import cn.miozus.gulimall.product.vo.AttrRespVo;
import cn.miozus.gulimall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


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

    @Autowired
    ProductAttrValueService productAttrValueService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * spu基地attr列表
     *
     * @param spuId spu id
     * @return {@link R}
     */
    @GetMapping("/base/listforspu/{spuId}")
    public R baseAttrListForSpu(@PathVariable("spuId") Long spuId) {
        // 查实体类一般都没有方法，原生都是批量查询
        List<ProductAttrValueEntity> entities = productAttrValueService.baseAttrListForSpu(spuId);

        return R.ok().put("data", entities);
    }

    /**
     * 更新基本attr列表spu
     *
     * @param spuId    spu id
     * @param entities 实体
     * @return {@link R}
     */
    @PostMapping("/update/{spuId}")
    public R updateBaseAttrListForSpu(@PathVariable("spuId") Long spuId,
                                      @RequestBody List<ProductAttrValueEntity> entities) {
        productAttrValueService.updateBaseAttrListForSpu(spuId, entities);

        return R.ok();
    }

    /**
     * 列表
     */
    @GetMapping("/{attrType}/list/{catlogId}")
    public R listBaseAttr(@RequestParam Map<String, Object> params,
                          @PathVariable("catlogId") Long catlogId,
                          @PathVariable("attrType") String attrType
    ) {
        PageUtils page = attrService.queryAttrPage(params, catlogId, attrType);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     *
     * @param attrId attr id
     * @return {@link R}
     */
    @Cacheable(value="attr", key="'attrInfo:'+#root.args[0]")
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
