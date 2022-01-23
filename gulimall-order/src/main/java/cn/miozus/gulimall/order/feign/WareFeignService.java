package cn.miozus.gulimall.order.feign;

import cn.miozus.common.utils.R;
import cn.miozus.gulimall.order.vo.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 调用库存服务
 *
 * @author miao
 * @date 2022/01/18
 */
@FeignClient("gulimall-ware")
public interface WareFeignService {
    /**
     * 批量查询商品是否有库存
     *
     * @param skuIds sku id
     * @return {@link R}
     */
    @PostMapping("/ware/waresku/hasStock")
    public R querySkuHasStock(@RequestBody List<Long> skuIds);

    /**
     * 查询票价
     *
     * @param addrId addr id
     * @return {@link R}
     */
    @GetMapping("/ware/wareinfo/fare")
    public R queryFare(@RequestParam("addrId") Long addrId) ;

    /**
     * 锁库存
     *
     * @param wareSkuLockVo 提供查询的字段
     * @return {@link R}
     */
    @PostMapping("/ware/waresku/lock")
    public R lockOrderStock(@RequestBody WareSkuLockVo wareSkuLockVo) ;

    }
