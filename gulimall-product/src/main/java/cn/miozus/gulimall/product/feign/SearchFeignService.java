package cn.miozus.gulimall.product.feign;

import cn.miozus.gulimall.common.to.es.SkuEsModel;
import cn.miozus.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 搜索服务
 *
 * @author miao
 * @date 2021/10/02
 */
@FeignClient("gulimall-search")
public interface SearchFeignService {

    @PostMapping("search/save/product")
    R publishProduct(@RequestBody List<SkuEsModel> skuEsModels);

}
