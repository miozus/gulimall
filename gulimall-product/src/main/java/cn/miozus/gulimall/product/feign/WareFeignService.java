package cn.miozus.gulimall.product.feign;


import cn.miozus.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 器皿装服务
 *
 * @author miao
 * @date 2021/10/02
 */
@FeignClient("gulimall-ware")
public interface WareFeignService {

    /**
     * 得到sku股票
     * 1️⃣ 可以加泛型：返回的R数据写上泛型，SB底层执行来回逆转JSON <-> POJO ；不加，默认Object 需要手动转型 ✅
     * 2️⃣ 直接返回 List<xxx>不担心手动转化（上帝视角提前知道里面封装的类型，而非程序检查类型）
     * 3️⃣ 自己封装解析解构 R ，R 内部提供方法， 逆转结果
     * 此处还要复制 ware 微服务的 VO 来这; 或者在公共类放一份，反正也要通过路由传输，当做 TO
     * @param skuIds sku id
     * @return {@link R}
     */
    @PostMapping("ware/waresku/hasStock")
    R getSkuHasStock(@RequestBody List<Long> skuIds);
}
