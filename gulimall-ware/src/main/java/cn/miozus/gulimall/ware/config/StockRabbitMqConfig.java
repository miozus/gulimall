package cn.miozus.gulimall.ware.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 库存释放监听器
 *
 * @author miao
 * @date 2022/01/24
 */
@Configuration
@Slf4j
public class StockRabbitMqConfig {

    public static final String EXCHANGE = "stock-event-exchange";
    public static final String DELAY_QUEUE = "stock.delay.queue";
    public static final String DELAY_QUEUE_ROUTING_KEY = "stock.lock";
    public static final int DELAY_QUEUE_TTL = (int) TimeUnit.MINUTES.toMillis(16L);
    public static final String RELEASE_ORDER_QUEUE = "stock.release.stock.queue";
    public static final String RELEASE_ORDER_ROUTING_KEY = "stock.release";


    /**
     * 延时队列流程图:库存
     *
     *                             Publisher
     *                                 │
     *            stock.lock  ┌────────▼───────────┐ stock.release  ┌─────────────────────────┐
     *          ┌─────────────┤stock-event-exchange├───────────────►│stock.release.stock.queue│
     *          │             └────────▲───────────┘                └───────────┬─────────────┘
     * ┌────────▼────────┐ stock.lock  │                                        │
     * │stock.delay.queue├─────────────┘                                        ▼
     * └─────────────────┘                                                   Consumer
     *
     * 下单服务 = 订单 + 库存
     * 设置错误不能覆盖设置，只能删掉重新缠绵
     */
    @Bean
    Exchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue delayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey(RELEASE_ORDER_ROUTING_KEY)
                .ttl(DELAY_QUEUE_TTL)
                .build();
    }

    @Bean
    Queue releaseQueue() {
        return new Queue(RELEASE_ORDER_QUEUE, true, false, false);
    }

    @Bean
    Binding createBinding(Queue delayQueue, TopicExchange exchange) {
        return BindingBuilder.bind(delayQueue).to(exchange).with(DELAY_QUEUE_ROUTING_KEY);
    }

    @Bean
    Binding releaseBinding(Queue releaseQueue, TopicExchange exchange) {
        return BindingBuilder.bind(releaseQueue).to(exchange).with(RELEASE_ORDER_ROUTING_KEY);
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
