package cn.miozus.gulimall.search.feign;

import cn.miozus.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * product feign
 *
 * @author miozus
 */
@FeignClient("gulimall-product")
public interface ProductFeignService {


    /**
     * attr info
     *
     * @param attrId attrId
     * @return {@link R}
     * @see R
     */
    @RequestMapping("/product/attr/info/{attrId}")
    public R attrInfo(@PathVariable("attrId") Long attrId);


}
