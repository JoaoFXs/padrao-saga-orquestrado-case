package br.com.microservices.orchestrated.orchestratorservice.core.consumer;


import br.com.microservices.orchestrated.orchestratorservice.core.service.OrchestratorService;

import br.com.microservices.orchestrated.orchestratorservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaOrchestratorConsumer {

    @Value("${spring.kafka.topic.start-saga}")
    private String startSagaTopic;
    @Value("${spring.kafka.topic.orchestrator}")
    private String orchestratorTopic;
    @Value("${spring.kafka.topic.finish-success}")
    private String finishSuccessTopic;
    @Value("${spring.kafka.topic.finish-fail}")
    private String finishFailTopic;

    private final JsonUtil jsonUtil;

    private final OrchestratorService orchestratorService;


    @KafkaListener(
            groupId = "orchestrator-group",
            topics = "start-saga"
    )
    public void consumeStartSagaEvent(String payload){
        log.info("Receiving start saga event {} from {} topic", payload, startSagaTopic);
        var event = jsonUtil.toEvent(payload);
        orchestratorService.startSaga(event);
        log.info("Event start saga {}", event);
    }

    @KafkaListener (
            groupId = "orchestrator-group",
            topics = "orchestrator"
    )
    public void consumeOrchestratorEvent(String payload){
        log.info("Receiving orchestrator event {} from {} topic", payload, orchestratorTopic);
        var event = jsonUtil.toEvent(payload);
        orchestratorService.continueSaga(event);
        log.info("Event orchestrator {}", event);
    }

    @KafkaListener (
            groupId = "orchestrator-group",
            topics = "finish-success"
    )
    public void consumeFinishSuccessEvent(String payload){
        log.info("Receiving finish success event {} from {} topic", payload, finishSuccessTopic);
        var event = jsonUtil.toEvent(payload);
        orchestratorService.finishSagaSuccess(event);
        log.info("Event finish success {}", event);
    }

    @KafkaListener (
            groupId = "orchestrator-group",
            topics = "finish-fail"
    )
    public void consumeFinishFailEvent(String payload){
        log.info("Receiving finish fail event {} from {} topic", payload, finishFailTopic);
        var event = jsonUtil.toEvent(payload);
        orchestratorService.finishSagaFail(event);
        log.info("Event finish fail {}", event);
    }
}
