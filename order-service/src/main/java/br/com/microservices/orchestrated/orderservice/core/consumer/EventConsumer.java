package br.com.microservices.orchestrated.orderservice.core.consumer;


import br.com.microservices.orchestrated.orderservice.core.service.EventService;
import br.com.microservices.orchestrated.orderservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final JsonUtil jsonUtil;

    private final EventService eventService;

    @Value("${spring.kafka.topic.notify-ending}")
    private String topic;

    @KafkaListener(
        groupId = "${spring.kafka.consumer.group-id}",
        topics = "${spring.kafka.topic.notify-ending}"
    )
    public void consumeNotifyEvent(String payload){
        log.info("Receiving ending notification event {} from {} topic", payload, topic);
        var event = jsonUtil.toEvent(payload);
        eventService.notifyEnding(event);
    }


}
