package cn.miozus.gulimall.order;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class GulimallOrderApplicationTests {

    @Autowired
    AmqpAdmin ampqAdmin;
    @Autowired
    RabbitTemplate rabbitTemplate;

    DirectExchange directExchange = new DirectExchange("java.direct");

    @Test
    void testExchangeAndQueuesAndBinding() {
        DirectExchange directExchange = new DirectExchange("java.direct");
        ampqAdmin.declareExchange(directExchange);
        log.info("directExchange {} ", directExchange.getName());

        Queue miozus = new Queue("miozus");
        log.info("miozus {} ", miozus);

        Binding binding = new Binding(miozus.getName(), Binding.DestinationType.QUEUE, directExchange.getName(), "miozus", null);
        ampqAdmin.declareBinding(binding);
        log.info("binding {} ", binding);
    }
    
    @Test
    void testRabbitTemplateSendString(){
        String s = "hello Rabbit";
        rabbitTemplate.convertAndSend(directExchange.getName(),"miozus", s);
        log.info("s {} ", s);

    }
}
