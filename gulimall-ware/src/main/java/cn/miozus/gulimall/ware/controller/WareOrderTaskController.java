package cn.miozus.gulimall.ware.controller;

import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.R;
import cn.miozus.gulimall.ware.entity.WareOrderTaskEntity;
import cn.miozus.gulimall.ware.service.WareOrderTaskService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;


/**
 * 库存工作单
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:20:54
 */
@RestController
@RequestMapping("ware/wareordertask")
public class WareOrderTaskController {
    @Autowired
    private WareOrderTaskService wareOrderTaskService;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @GetMapping("/mq/testDelayQueue")
    @ResponseBody
    public String testDelayQueue() {
        WareOrderTaskEntity entity = new WareOrderTaskEntity();
        String uuid = UUID.randomUUID().toString();
        entity.setOrderSn(uuid);
        entity.setCreateTime(new Date());

        rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",entity);
        return "ok";
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareOrderTaskService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		WareOrderTaskEntity wareOrderTask = wareOrderTaskService.getById(id);

        return R.ok().put("wareOrderTask", wareOrderTask);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody WareOrderTaskEntity wareOrderTask){
		wareOrderTaskService.save(wareOrderTask);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody WareOrderTaskEntity wareOrderTask){
		wareOrderTaskService.updateById(wareOrderTask);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		wareOrderTaskService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
