package br.com.microservices.orchestrated.inventoryservice.core.consumer;



import br.com.microservices.orchestrated.inventoryservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryConsumer {

    @Value("${spring.kafka.topic.inventory-success}")
    private String inventorySuccess;

    @Value("${spring.kafka.topic.inventory-fail}")
    private String inventoryFail;

    private final JsonUtil jsonUtil;

    @KafkaListener(
        groupId = "inventory-group",
        topics = "inventory-success"
    )
    public void consumeInventorySuccessEvent(String payload){
        log.info("Receiving inventory success event {} from {} topic", payload, inventorySuccess);
        var event = jsonUtil.toEvent(payload);
        log.info("Event inventory success {}", event);
    }

    @KafkaListener(
            groupId = "inventory-group",
            topics = "inventory-fail"
    )
    public void consumeInventoryFailEvent(String payload){
        log.info("Receiving rollback event {} from {} topic", payload, inventoryFail);
        var event = jsonUtil.toEvent(payload);
        log.info("Event inventory fail {}", event);
    }
}
