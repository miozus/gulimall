package cn.miozus.gulimall.order.listener;

import cn.miozus.gulimall.common.to.mq.SeckillOrderTo;
import cn.miozus.gulimall.order.config.RabbitMqOrderConfig;
import cn.miozus.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;


/**
 * 订单关闭监听器
 *
 * @author miao
 * @date 2022/01/25
 */
@Slf4j
@Service
@RabbitListener(queues = RabbitMqOrderConfig.RELEASE_ORDER_QUEUE)
public class OrderSeckillListener {

    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void handleStockLockedReleaseListener(SeckillOrderTo to, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        log.info("📝准备创建秒杀订单的详细信息");
        try {
            orderService.createSeckillOrder(to);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicReject(deliveryTag, true);
            e.printStackTrace();
        }

    }
}

