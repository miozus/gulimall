package cn.miozus.gulimall.ware.listener;

import cn.miozus.gulimall.common.to.mq.OrderTo;
import cn.miozus.gulimall.common.to.mq.StockLockedUndoLogTo;
import cn.miozus.gulimall.ware.config.RabbitMqStockConfig;
import cn.miozus.gulimall.ware.service.WareSkuService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 死信队列监听器
 *
 * @author miao
 * @date 2022/01/24
 */
@Slf4j
@Service
@RabbitListener(queues = RabbitMqStockConfig.RELEASE_ORDER_QUEUE)
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;

    @RabbitHandler
    public void handleStockReleaseListener(StockLockedUndoLogTo to, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("********************** 听到库存解锁的消息: 事务回滚的主动解锁 *******************************");
        try {
            wareSkuService.unlockOrderStock(to);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicReject(deliveryTag, true);
            e.printStackTrace();
        }
    }

    @RabbitHandler
    public void handleStockReleaseAfterCancelOrderListener(OrderTo to, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("********************* 听到库存解锁的消息：订单取消的被动解锁（二次确认）******************************");
        try {
            wareSkuService.unlockOrderStock(to);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicReject(deliveryTag, true);
            e.printStackTrace();
        }
    }
}
