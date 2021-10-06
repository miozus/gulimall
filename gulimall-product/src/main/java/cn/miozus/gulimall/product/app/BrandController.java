package cn.miozus.gulimall.product.app;

import java.util.Arrays;
import java.util.Map;

import cn.miozus.common.valid.AddGroup;
import cn.miozus.common.valid.UpdateGroup;
import cn.miozus.common.valid.UpdateStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.miozus.gulimall.product.entity.BrandEntity;
import cn.miozus.gulimall.product.service.BrandService;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.R;



/**
 * 品牌
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-06 23:57:18
 */
@RestController
@RequestMapping("product/brand")
public class BrandController {
    @Autowired
    private BrandService brandService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = brandService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    public R info(@PathVariable("brandId") Long brandId) {
        BrandEntity brand = brandService.getById(brandId);

        return R.ok().put("brand", brand);
    }

    /**
     * 保存：收集校验错误信息，塞进响应报文
     *
     * @param brand 品牌
     * @return {@link R}
     */
    @RequestMapping("/save")
    public R save(@Validated(AddGroup.class) @RequestBody BrandEntity brand/*, BindingResult result*/) {
        //if (result.hasErrors()) {
        //    Map<String, String> map = new HashMap<>();
        //    result.getFieldErrors().forEach(item -> {
        //        String message = item.getDefaultMessage();
        //        String field = item.getField();
        //        map.put(field, message);
        //    });
        //    return R.error(400, "提交的数据不合法").put("data", map);
        //} else {
        //    brandService.save(brand);
        //}
        brandService.save(brand);
        return R.ok();
    }

    /**
     * 修改显示状态；为了（保留）排查问题带出名字，所以单独设置
     */
    @RequestMapping("/update/status")
    public R updateStatus(@Validated(UpdateStatus.class) @RequestBody BrandEntity brand) {
        brandService.updateById(brand);

        return R.ok();
    }
    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@Validated(UpdateGroup.class) @RequestBody BrandEntity brand) {
        brandService.updateDetails(brand);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] brandIds) {
        brandService.removeByIds(Arrays.asList(brandIds));

        return R.ok();
    }

}
