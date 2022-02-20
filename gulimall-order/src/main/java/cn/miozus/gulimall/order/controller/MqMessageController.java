package cn.miozus.gulimall.order.controller;

import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.R;
import cn.miozus.gulimall.order.entity.MqMessageEntity;
import cn.miozus.gulimall.order.service.MqMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;



/**
 * 
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:18:03
 */
@RestController
@RequestMapping("order/mqmessage")
public class MqMessageController {
    @Autowired
    private MqMessageService mqMessageService;

    /** 列表 */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = mqMessageService.queryPage(params);
        return R.ok().put("page", page);
    }


    /** 信息 */
    @RequestMapping("/info/{messageId}")
    public R info(@PathVariable("messageId") String messageId){
		MqMessageEntity mqMessage = mqMessageService.getById(messageId);
        return R.ok().put("mqMessage", mqMessage);
    }

    /** 保存 */
    @RequestMapping("/save")
    public R save(@RequestBody MqMessageEntity mqMessage){
		mqMessageService.save(mqMessage);
        return R.ok();
    }

    /** 修改 */
    @RequestMapping("/update")
    public R update(@RequestBody MqMessageEntity mqMessage){
		mqMessageService.updateById(mqMessage);
        return R.ok();
    }

    /** 删除 */
    @RequestMapping("/delete")
    public R delete(@RequestBody String[] messageIds){
		mqMessageService.removeByIds(Arrays.asList(messageIds));
        return R.ok();
    }

}
