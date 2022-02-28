package cn.miozus.gulimall.order.controller;

import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.order.entity.OrderEntity;
import cn.miozus.gulimall.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;



/**
 * 订单
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:18:03
 */
@RestController
@RequestMapping("order/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    /** 列表 */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = orderService.queryPage(params);
        return R.ok().put("page", page);
    }

    /**
     * 列表，包含购物车商品详情
     * RequestBody: JSON 格式传输比TEXT 好
     */
    @PostMapping("/listWithItems")
    public R listWithItems(@RequestBody Map<String, Object> params){
        PageUtils page = orderService.queryPageWithItems(params);
        return R.ok().put("page", page);
    }

    /** 信息 */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
        OrderEntity order = orderService.getById(id);
        return R.ok().put("order", order);
    }

    /** 信息 */
    @GetMapping("/SN/{sn}")
    public R queryOrderStatus(@PathVariable("sn") String orderSn){
		OrderEntity order = orderService.queryOrderBySn(orderSn);
        return R.ok().setData(order);
    }

    /** 保存 */
    @RequestMapping("/save")
    public R save(@RequestBody OrderEntity order){
		orderService.save(order);
        return R.ok();
    }

    /** 修改 */
    @RequestMapping("/update")
    public R update(@RequestBody OrderEntity order){
		orderService.updateById(order);
        return R.ok();
    }

    /** 删除 */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		orderService.removeByIds(Arrays.asList(ids));
        return R.ok();
    }

}
