package cn.miozus.gulimall.order.controller;

import cn.miozus.gulimall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Controller;

/**
 * 兔子mq消费者
 *
 * @author miao
 * @date 2022/01/13
 */
@Controller
@Slf4j
@RabbitListener(queues = {"miozus"})
public class RabbitMqConsumer {

    /**
     * 消费者：接收消息
     *
     * AutomaticAck: （默认）货到自动签收 + 不放回
     * <p>
     * 势不可挡：即使突然中止服务，消费仍会进行，所以推荐设置要条件再签收（也许要阻塞或方法短路）
     * 阴魂不散：下次服务上线，消息代理又重新投递
     * 无量子态：Unacked，只要代理未收到签收/拒收回执，就储存队列，所以处理完业务必须表态
     * 是否放生：requeue, 放回队列后，拒收+不放回=丢弃销毁
     *
     * 形而下学：Object ⇒ Message
     * @param message 消息 原生方法体=头部+成对配置，org.springframework.amqp.core.Message
     * @param entity  实体 可选参数，直接转换为对象
     * @param channel 通道 可选参数，操作高速通道： basic+ [Ack 手动签收， Nack 拒收（支持批量），Reject 拒收]
     * @Annotation RabbitListener 类上监听队列
     * @Annotation RabbitHandler 重载方法，分类不同的实体类
     */
    @SneakyThrows
    @RabbitHandler
    public void receiveMessage(Message message, OrderReturnReasonEntity entity, Channel channel) {

        MessageProperties properties = message.getMessageProperties();
        long tag = properties.getDeliveryTag();
        log.info("🐰 " + tag + "号快递员，来到了你家楼下，包裹 {} ", entity);

        if (tag % 2 == 0) {
            channel.basicAck(tag, true);
            log.info("✅ 已签收 {} ", tag);
        } else {
            channel.basicNack(tag, false, true);
            log.info("❌ 拒收 {} ", tag);
        }
        log.info("❓ 未签收 {} ", tag);

    }


}
