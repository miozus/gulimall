package cn.miozus.gulimall.seckill.aspect;

import cn.miozus.gulimall.common.annotation.PostRabbitMq;
import cn.miozus.gulimall.common.to.mq.SeckillOrderTo;
import cn.miozus.gulimall.seckill.config.SeckillRabbitMqConfig;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 订单消息队列切面
 *
 * @author miozus
 * @date 2022/02/17
 */
@Aspect
@Component
@Order(2)
@Slf4j
public class SeckillRabbitMqAspect {

    @Lazy
    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 快速创建订单：发送消息创建完成
     */
    @After(value = "@annotation(postRabbitMq)")
    public Object sendRabbitMqAfterSeckill(JoinPoint point,  PostRabbitMq postRabbitMq) {
        SeckillOrderTo to = (SeckillOrderTo) point.getArgs()[0];
        rabbitTemplate.convertAndSend(
                SeckillRabbitMqConfig.EXCHANGE, SeckillRabbitMqConfig.DELAY_QUEUE_SECKILL_ROUTING_KEY, to);
        log.info("快速创建订单：发送消息创建完成: "+ to.getOrderSn());
        return to.getOrderSn();
    }
}
