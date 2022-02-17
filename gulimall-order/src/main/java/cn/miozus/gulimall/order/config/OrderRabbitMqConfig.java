package cn.miozus.gulimall.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 兔子消息队列配置
 *
 * @author miao
 * @date 2022/01/13
 */
@Slf4j
@Configuration
public class OrderRabbitMqConfig {

    @Autowired
    RabbitTemplate rabbitTemplate;

    public static final String EXCHANGE = "order-event-exchange";
    public static final String DELAY_QUEUE = "order.delay.queue";
    public static final int DELAY_QUEUE_TTL = (int) TimeUnit.MINUTES.toMillis(15L);
    public static final String DELAY_QUEUE_ROUTING_KEY = "order.create.order";
    public static final String RELEASE_ORDER_QUEUE = "order.release.order.queue";
    public static final String RELEASE_ORDER_ROUTING_KEY = "order.release.order";
    public static final String RELEASE_STOCK_QUEUE = "stock.release.stock.queue";
    public static final String RELEASE_OTHER_ROUTING_KEY = "order.release.other";
    private static final String RELEASE_OTHER_THEME_ROUTING_KEY = "order.release.other.#";


    /**
     * 延时队列流程图：订单
     *
     *                                       Publisher                                       Consumer
     *                                           │                                               ▲
     *                                           │                                               │
     *             order.create.order   ┌────────▼───────────┐order.release.order    ┌───────────┴─────────────┐
     *          ┌───────────────────────┤order-event-exchange├──────────────────────►│order.release.order.queue│
     *          │                       └────────▲────────┬──┘                       └─────────────────────────┘
     *          │                                │        │
     * ┌────────▼────────┐  order.release.order  │        │   order.release.other.#  ┌─────────────────────────┐
     * │order.delay.queue├───────────────────────┘        └─────────────────────────►│stock.release.stock.queue│
     * └─────────────────┘                                                           └───────────┬─────────────┘
     *                                                                                           │
     *                                                                                           ▼
     *                                                                                       Consumer
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
    Queue releaseStockQueue() {
        return new Queue(RELEASE_STOCK_QUEUE, true, false, false);
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
     * 订单释放其他绑定
     *
     * 比如双重解锁库存
     *
     * @param releaseStockQueue 订单释放订单队列
     * @param exchange               交换机
     * @return {@link Binding}
     */
    @Bean
    Binding releaseOtherBinding(Queue releaseStockQueue, TopicExchange exchange) {
        return BindingBuilder.bind(releaseStockQueue).to(exchange).with(RELEASE_OTHER_THEME_ROUTING_KEY);
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

    /**
     * 可靠投递： 初始化消息回执
     * 加载完后执行此方法
     */
    @PostConstruct
    void initPublisherConfirms() {

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
