package cn.miozus.gulimall.ware.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 兔子mq配置
 *
 * @author miao
 * @date 2022/01/24
 */
@Configuration
@Slf4j
public class RabbitMqConfig {

    @Autowired
    RabbitTemplate rabbitTemplate;

    public static final String STOCK_EVENT_EXCHANGE = "stock-event-exchange";
    public static final String STOCK_DELAY_QUEUE = "stock.delay.queue";
    public static final String STOCK_DELAY_QUEUE_ROUTING_KEY = "stock.lock";
    public static final Integer STOCK_DELAY_QUEUE_TTL = 2 * 60 * 1000;
    public static final String STOCK_RELEASE_ORDER_QUEUE = "stock.release.stock.queue";
    public static final String STOCK_RELEASE_ORDER_ROUTING_KEY = "stock.release";


    /**
     *                                   Publisher
     *                                       │
     *               stock.lock     ┌────────▼───────────┐   stock.release   ┌───────────────────────────┐
     *          ┌───────────────────┤stock-event-exchange├──────────────────►│ stock.release.stock.queue │
     *          │                   └────────────────────┘                   └────────────┬──────────────┘
     * ┌────────▼────────┐                   ▲                                            │
     * │stock.delay.queue├───────────────────┘                                            ▼
     * └─────────────────┘    stock.lock                                               Consumer
     *
     *
     * 延时队列流程图
     * 下单服务 = 订单 + 库存
     * 设置错误不能覆盖设置，只能删掉重新缠绵
     */
    @Bean
    Exchange exchange() {
        return new TopicExchange(STOCK_EVENT_EXCHANGE, true, false);
    }

    @Bean
    Queue orderDelayQueue() {
        return QueueBuilder.durable(STOCK_DELAY_QUEUE)
                .deadLetterExchange(STOCK_EVENT_EXCHANGE)
                .deadLetterRoutingKey(STOCK_RELEASE_ORDER_ROUTING_KEY)
                .ttl(STOCK_DELAY_QUEUE_TTL)
                .build();
    }

    @Bean
    Queue orderReleaseOrderQueue() {
        return new Queue(STOCK_RELEASE_ORDER_QUEUE, true, false, false);
    }

    @Bean
    Binding orderCreateBinding(Queue orderDelayQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderDelayQueue).to(exchange).with(STOCK_DELAY_QUEUE_ROUTING_KEY);
    }

    @Bean
    Binding orderReleaseBinding(Queue orderReleaseOrderQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderReleaseOrderQueue).to(exchange).with(STOCK_RELEASE_ORDER_ROUTING_KEY);
    }

    /**
     * 消息转换器：JSON & Object
     *
     * @return {@link MessageConverter}
     */
    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }


}
