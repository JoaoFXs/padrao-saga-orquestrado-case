package br.com.microservices.orchestrated.paymentservice.core.consumer;


import br.com.microservices.orchestrated.paymentservice.core.service.PaymentService;
import br.com.microservices.orchestrated.paymentservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    @Value("${spring.kafka.topic.payment-success}")
    private String topicValidationSuccess;

    @Value("${spring.kafka.topic.payment-fail}")
    private String productValidationFail;

    private final JsonUtil jsonUtil;
    private final PaymentService paymentService;
    @KafkaListener(
        groupId = "payment-group",
        topics = "payment-success"
    )
    public void consumePaymentSuccessEvent(String payload){
        log.info("Receiving payment success event {} from {} topic", payload, topicValidationSuccess);
        paymentService.realizePayment(jsonUtil.toEvent(payload));
        var event = jsonUtil.toEvent(payload);
        log.info("Event product validation success {}", event);
    }

    @KafkaListener(
            groupId = "payment-group",
            topics = "payment-fail"
    )
    public void consumePaymentFailEvent(String payload){
        log.info("Receiving rollback event {} from {} topic", payload, productValidationFail);
        paymentService.realizeRefund(jsonUtil.toEvent(payload));
        var event = jsonUtil.toEvent(payload);
        log.info("Event payment fail {}", event);
    }
}
