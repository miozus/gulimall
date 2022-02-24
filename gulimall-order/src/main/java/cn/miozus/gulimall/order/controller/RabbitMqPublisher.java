package cn.miozus.gulimall.order.controller;

import cn.miozus.gulimall.order.entity.OrderEntity;
import cn.miozus.gulimall.order.entity.OrderReturnReasonEntity;
import com.sun.istack.internal.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.UUID;

/**
 * 兔子mq测试
 *
 * @author miao
 * @date 2022/01/13
 */
@Controller
@Slf4j
@ResponseBody
public class RabbitMqPublisher {

    @Autowired
    RabbitTemplate rabbitTemplate;

    DirectExchange directExchange = new DirectExchange("java.direct");

    /**
     * 多次测试兔子模板发送对象
     * <p>
     * 特性：
     * 1️⃣ 丢绣球：queues: 支持多人监听消费，消费一个收一个（库存有限），而且只能让一个消费者收到消息
     * 客户端A：1,4,7
     * 客户端B：0,3,6,9
     * 消息丢失？：2,5,8 被单元测试启动时抢到了（启动日志搜到）
     * <p>
     * 2️⃣ 线程安全：  [接收-处理] 是绑定操作，只有消费完这个，才能接收下一个
     * 测试条件同1️⃣，增加打印结果处理前后，间隔 3 秒，结果呈现 BBAADD 格式
     */
    @GetMapping("/mq/sendMsg")
    public String testRabbitTemplateSendObjectManyTimes(@RequestParam(value = "num", defaultValue = "5") Integer num) {
        for (int i = 0; i < num; i++) {
            testRabbitTemplateSendObject(i);
        }
        return "ok";
    }

    /**
     * 测试延迟队列
     * 模拟下单成功
     * @return {@link String}
     */
    @GetMapping("/mq/testDelayQueue")
    public String testDelayQueue() {
        OrderEntity entity = new OrderEntity();
        String uuid = UUID.randomUUID().toString();
        entity.setOrderSn(uuid);
        entity.setModifyTime(new Date());

        rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",entity);
        return "ok";
    }


    /**
     * 测试兔子模板发送对象
     * <p>
     * 传输前后：对象必须实现序列化
     * <p>
     * correlationData: 每条消息的身份证，uuid 可储存到 MySQL，留下痕迹查询是否收到
     *
     * @return
     */
    public void testRabbitTemplateSendObject(@Nullable Integer count) {
        OrderReturnReasonEntity order = new OrderReturnReasonEntity();
        order.setId(5672L);
        order.setName("rabbit" + count);
        String uuid = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(uuid);
        rabbitTemplate.convertAndSend(directExchange.getName(), "miozus", order, correlationData);
        log.info("order {} ", order);
    }
}
