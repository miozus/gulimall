package cn.miozus.gulimall.order.listener;

import cn.miozus.gulimall.order.config.OrderRabbitMqConfig;
import cn.miozus.gulimall.order.entity.OrderEntity;
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
@RabbitListener(queues = OrderRabbitMqConfig.RELEASE_ORDER_QUEUE)
public class OrderCloseListener {

    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void handleStockLockedReleaseListener(OrderEntity to, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        log.info("************************听到订单关闭的消息********************************");
        try {
            orderService.closeOrder(to);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicReject(deliveryTag, true);
            e.printStackTrace();
        }

    }
}

