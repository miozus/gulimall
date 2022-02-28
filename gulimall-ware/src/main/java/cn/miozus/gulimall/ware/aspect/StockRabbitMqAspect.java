package cn.miozus.gulimall.ware.aspect;

import cn.miozus.gulimall.common.annotation.PostRabbitMq;
import cn.miozus.gulimall.common.to.mq.StockLockedUndoLogTo;
import cn.miozus.gulimall.ware.config.RabbitMqStockConfig;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 库存消息队列切面
 *
 * @author miozus
 * @date 2022/02/18
 */
@Aspect
@Component
public class StockRabbitMqAspect {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @AfterReturning(value = "@annotation(postRabbitMq)", returning = "retVal")
    public Object sendRabbitMq(JoinPoint point, Object retVal, PostRabbitMq postRabbitMq) {
        StockLockedUndoLogTo undoLogTo = (StockLockedUndoLogTo) retVal;
        rabbitTemplate.convertAndSend(RabbitMqStockConfig.EXCHANGE,
                RabbitMqStockConfig.DELAY_QUEUE_ROUTING_KEY,
                undoLogTo);
        return retVal;
    }
}
