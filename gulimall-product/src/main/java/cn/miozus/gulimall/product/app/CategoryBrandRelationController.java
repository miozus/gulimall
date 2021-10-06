package cn.miozus.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.miozus.gulimall.product.entity.BrandEntity;
import cn.miozus.gulimall.product.vo.BrandVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import cn.miozus.gulimall.product.entity.CategoryBrandRelationEntity;
import cn.miozus.gulimall.product.service.CategoryBrandRelationService;
import cn.miozus.common.utils.R;


/**
 * 品牌分类关联
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-06 23:57:18
 */
@RestController
@RequestMapping("product/categorybrandrelation")
public class CategoryBrandRelationController {
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    /**
     * 列表：当前品牌关联的所有分类列表
     */
    @GetMapping("/catalog/list")
    public R catalogList(@RequestParam("brandId") Long brandId) {
        List<CategoryBrandRelationEntity> data = categoryBrandRelationService.list(
                new QueryWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId)
        );

        return R.ok().put("data", data);
    }

    /**
     * 品牌关系列表
     * 职责分工(C 三句话）
     * Controller            >   Service          >   Controller
     * 处理请求，接受和校验数据    接收数据，业务处理        封装页面指定vo
     *
     * @param catId catId
     * @return {@link R}
     * @see R
     */
    @GetMapping("/brands/list")
    public R relationBrandList(@RequestParam("catId") Long catId) { // 接收数据和数据校验
        // 奇怪的设定：不直接从关联表查，非要做中转; 老师说为了方便其他人抄家（品牌表查全）
        // 传递给 Service
        List<BrandEntity> vos = categoryBrandRelationService.getBrandListByCatId(catId);
        // 封装页面指定Vo
        List<BrandVo> data = vos.stream().map(vo -> {
                    // 品牌表：name 关联表：brandName 因此不是能做属性拷贝
                    BrandVo brandVo = new BrandVo();
                    brandVo.setBrandId(vo.getBrandId());
                    brandVo.setBrandName(vo.getName());
                    return brandVo;
                }
        ).collect(Collectors.toList());

        return R.ok().put("data", data);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        CategoryBrandRelationEntity categoryBrandRelation = categoryBrandRelationService.getById(id);

        return R.ok().put("categoryBrandRelation", categoryBrandRelation);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody CategoryBrandRelationEntity categoryBrandRelation) {
        // 前端只传2个字段，未传入其他字段（冗余字段），所以不用原生方法
        // 而且导致每次从其他表关联查询，性能影响大
        // 电商系统的大表数据，从不做关联！哪怕一点一点查，也不用关联。
        categoryBrandRelationService.saveDetails(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody CategoryBrandRelationEntity categoryBrandRelation) {
        categoryBrandRelationService.updateById(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        categoryBrandRelationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
