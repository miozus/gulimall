package cn.miozus.gulimall.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 兔子mq配置
 *
 * @author miao
 * @date 2022/01/13
 */
@Configuration
@Slf4j
public class RabbitMqConfig {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 可靠投递： 初始化消息回执
     */
    @PostConstruct
    public void initPublisherConfirms() {

        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             * 已发送回执
             *
             * 生产者：服务收到消息就回调回调
             *
             * 设置两个参数开启
             * spring.rabbitmq.publisher-confirms=true
             * spring.rabbitmq.publisher-confirm-type=correlated
             *
             * @param correlationData 关联数据，唯一身份证
             * @param ack             发送成功回执：只要发送给了消息代理就真
             * @param cause           失败原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                log.info("📨 已发送, params: {} ", "correlationData:" + correlationData + "," + "ack:" + ack + ","
                        + "cause:" + cause);
            }
        });


        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {

            /**
             * 投递失败回执
             *
             * 生产者：消息正确抵达队列就回调
             *
             * 失败时触发，但留下蛛丝马迹可供溯源
             *
             * # 开启发送端抵达队列的确认
             * spring.rabbitmq.publisher-returns=true
             * # 只要抵达队列，以异步发送优先回调
             * spring.rabbitmq.template.mandatory=true
             *
             * @param message    消息
             * @param replyCode  回复代码
             * @param replyText  回复文本
             * @param exchange   交换机
             * @param routingKey 暗号
             */
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                log.info("❌ 投递失败, params: {} ", "message:" + message + "," + "replyCode:"
                        + replyCode + "," + "replyText:" + replyText + "," + "exchange:" + exchange + ","
                        + "routingKey:" + routingKey);
            }
        });
    }


}
