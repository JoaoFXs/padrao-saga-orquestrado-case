package br.com.microservices.orchestrated.orchestratorservice.core.producer;

import br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


/** Classe criada para trabalhar com produtores**/
@Slf4j//Anotação de logs
@Component
@AllArgsConstructor
public class SagaOrchestratorProducer {
    /** Isso funciona por conta do KafkaConfig realizado**/
    private final KafkaTemplate<String, String> kafkaTemplate;

    /** Método para envio de evento **/
    public void sendEvent(String payload, String topico){
        try{
            log.info("Sending event to topic {} with data {}", topico, payload);
            kafkaTemplate.send(topico, payload);
        } catch (Exception e) {
            log.error("Error trying to send data to topic {} with data {}", topico, payload, e);
        }
    }
}
