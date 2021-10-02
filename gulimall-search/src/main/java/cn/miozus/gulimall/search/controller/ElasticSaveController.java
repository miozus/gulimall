package cn.miozus.gulimall.search.controller;


import cn.miozus.common.exception.BizCodeEnum;
import cn.miozus.common.to.es.SkuEsModel;
import cn.miozus.common.utils.R;
import cn.miozus.gulimall.search.controller.Service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 弹性节省控制器
 *
 * @author miao
 * @date 2021/10/02
 */
@RestController
@RequestMapping("/search/save")
@Slf4j
public class ElasticSaveController {

    @Autowired
    ProductSaveService productSaveService;

    /**
     * 发布产品
     *
     * @param skuEsModels sku
     * @return {@link R}
     */
    @PostMapping("/product")
    public R publishProduct(@RequestBody List<SkuEsModel> skuEsModels) {
        boolean hasFailure = false;
        try {
            // 返回 true 意味着 hasFailure 成立
            hasFailure = productSaveService.publish(skuEsModels);
        } catch (Exception e) {
            // 可能某个 Es 数据有问题
            log.error("ElasticSaveController 商品上架错误 {}", e);
            // 错误码和错误消息，统一放到公共服务中管理；可能连不上
            return R.error(
                    BizCodeEnum.PUBLISH_EXCEPTION.getCode(),
                    BizCodeEnum.PUBLISH_EXCEPTION.getMsg()
            );
        }
        if (hasFailure) {
            // 错误码和错误消息，统一放到公共服务中管理；可能连不上
            return R.error(
                    BizCodeEnum.PUBLISH_EXCEPTION.getCode(),
                    BizCodeEnum.PUBLISH_EXCEPTION.getMsg()
            );
        } else {
            return R.ok();
        }
    }
}
