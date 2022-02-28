package cn.miozus.gulimall.order.aspect;

import cn.miozus.gulimall.common.annotation.PostRabbitMq;
import cn.miozus.gulimall.common.to.mq.OrderTo;
import cn.miozus.gulimall.order.config.RabbitMqOrderConfig;
import cn.miozus.gulimall.order.entity.OrderEntity;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
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
public class OrderRabbitMqAspect {

    @Lazy
    @Autowired
    RabbitTemplate rabbitTemplate;

    @AfterReturning(value = "@annotation(postRabbitMq)", returning = "retVal")
    public Object sendRabbitMq(JoinPoint point, Object retVal, PostRabbitMq postRabbitMq) {
        if (Objects.nonNull(retVal)) {
            // 创建订单：发送消息创建完成
            OrderEntity order = (OrderEntity) retVal;
            pushDelayQueueAfterSubmitOrder(order);
        } else {
            // 关闭订单：二次确认解锁库存
            OrderEntity methodArg = (OrderEntity) point.getArgs()[0];
            pushReleaseQueueAfterCancelOrderForSure(methodArg);
        }
        return retVal;
    }

    private void pushReleaseQueueAfterCancelOrderForSure(OrderEntity methodArg) {
        OrderTo orderTo = new OrderTo();
        BeanUtils.copyProperties(methodArg, orderTo);
        rabbitTemplate.convertAndSend(RabbitMqOrderConfig.EXCHANGE,
                RabbitMqOrderConfig.RELEASE_OTHER_QUEUE_ROUTING_KEY, orderTo);
    }

    private void pushDelayQueueAfterSubmitOrder(OrderEntity order) {
        rabbitTemplate.convertAndSend(RabbitMqOrderConfig.EXCHANGE, RabbitMqOrderConfig.DELAY_QUEUE_ROUTING_KEY, order);
    }
}
