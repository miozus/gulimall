package cn.miozus.gulimall.ware.listener;

import cn.miozus.common.to.stock.StockLockedUndoLogTo;
import cn.miozus.gulimall.ware.config.RabbitMqConfig;
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
@Service
@RabbitListener(queues = RabbitMqConfig.STOCK_RELEASE_ORDER_QUEUE)
@Slf4j
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;

    @RabbitHandler
    public void handleStockLockedReleaseListener(StockLockedUndoLogTo to, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        log.info("************************收到库存解锁的消息********************************");
        try {
            wareSkuService.unlockOrderStock(to);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicReject(deliveryTag, true);
            e.printStackTrace();
        }

    }
}