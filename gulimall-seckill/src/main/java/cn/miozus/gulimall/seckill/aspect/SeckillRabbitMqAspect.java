package cn.miozus.gulimall.seckill.aspect;

import cn.miozus.gulimall.common.annotation.PostRabbitMq;
import cn.miozus.gulimall.common.to.mq.SeckillOrderTo;
import cn.miozus.gulimall.seckill.config.RabbitMqSeckillConfig;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 订单消息队列切面
 *
 * @author miozus
 * @date 2022/02/17
 */
@Aspect
@Component
@Order(2)
public class SeckillRabbitMqAspect {

    @Lazy
    @Autowired
    RabbitTemplate rabbitTemplate;

    @AfterReturning(value = "@annotation(postRabbitMq)", returning = "retVal")
    public Object sendRabbitMqAfterSeckill(JoinPoint point, Object retVal, PostRabbitMq postRabbitMq) {
        if (Objects.nonNull(retVal)) {
            SeckillOrderTo to = (SeckillOrderTo) point.getArgs()[0];
            String orderSn = (String) retVal;
            to.setOrderSn(orderSn);
            // 快速创建订单：发送消息创建完成
            rabbitTemplate.convertAndSend(
                    RabbitMqSeckillConfig.EXCHANGE, RabbitMqSeckillConfig.DELAY_QUEUE_SECKILL_ROUTING_KEY, to);
        }
        return retVal;
    }
}
