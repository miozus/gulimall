package cn.miozus.gulimall.product.app;

import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.product.entity.SpuInfoEntity;
import cn.miozus.gulimall.product.service.SpuInfoService;
import cn.miozus.gulimall.product.vo.SpuSaveVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;


/**
 * spu信息
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-06 23:57:18
 */
@RestController
@RequestMapping("product/spuinfo")
public class SpuInfoController {
    @Autowired
    private SpuInfoService spuInfoService;


    /**
     * 根据skuId查询spu信息
     * 用于购物车封装
     *
     * @param skuId sku id
     * @return {@link R}
     */
    @PostMapping("/bySkuId/{skuId}")
    public R querySpuInfoBySkuId(@PathVariable("skuId") Long skuId) {
        SpuInfoEntity spuInfo = spuInfoService.querySpuInfoBySkuId(skuId);
        return R.ok().put("spuInfo", spuInfo);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {

        PageUtils page = spuInfoService.queryPageByCondition(params);

        return R.ok().put("page", page);
    }

    @PostMapping("/{spuId}/up")
    public R publish(@PathVariable("spuId") Long spuId) {

        spuInfoService.publish(spuId);
        return R.ok();
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        SpuInfoEntity spuInfo = spuInfoService.getById(id);

        return R.ok().put("spuInfo", spuInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody SpuSaveVo vo) {
        //spuInfoService.save(vo);
        // 牵扯太多，准备开始大保存
        spuInfoService.saveSpuInfo(vo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody SpuInfoEntity spuInfo) {
        spuInfoService.updateById(spuInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        spuInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
