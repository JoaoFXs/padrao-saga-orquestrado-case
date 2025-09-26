package br.com.microservices.orchestrated.productvalidationservice.core.consumer;

import br.com.microservices.orchestrated.productvalidationservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductValidationConsumer {

    @Value("${spring.kafka.topic.product-validation-success}")
    private String topicValidationSuccess;

    @Value("${spring.kafka.topic.product-validation-fail}")
    private String productValidationFail;

    private final JsonUtil jsonUtil;

    @KafkaListener(
        groupId = "product-validation-group",
        topics = "product-validation-success"
    )
    public void consumeProductValidationSuccessEvent(String payload){
        log.info("Receiving product validation success event {} from {} topic", payload, topicValidationSuccess);
        var event = jsonUtil.toEvent(payload);
        log.info("Event product validation success {}", event);
    }

    @KafkaListener(
            groupId = "product-validation-group",
            topics = "product-validation-fail"
    )
    public void consumeProductValidationFailEvent(String payload){
        log.info("Receiving rollback event {} from {} topic", payload, productValidationFail);
        var event = jsonUtil.toEvent(payload);
        log.info("Event product validation fail {}", event);
    }
}
