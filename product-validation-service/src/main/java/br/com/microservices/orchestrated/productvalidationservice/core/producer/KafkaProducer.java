package br.com.microservices.orchestrated.productvalidationservice.core.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


/** Classe criada para trabalhar com produtores**/
@Slf4j//Anotação de logs
@Component
@RequiredArgsConstructor//Utilizado para instanciar apenas o que for necessario de instanciação
public class KafkaProducer {
    /** Isso funciona por conta do KafkaConfig realizado**/
    private final KafkaTemplate<String, String> kafkaTemplate;


    /** Topico produtor **/
    @Value("${spring.kafka.topic.orchestrator}")
    private String orchestratorTopic;

    /** Método para envio de evento **/
    public void sendEvent(String payload){
        try{
            log.info("Sending event to topic {} with data {}", orchestratorTopic, payload);
            kafkaTemplate.send(orchestratorTopic, payload);
        } catch (Exception e) {
            log.error("Error trying to send data to topic {} with data {}", orchestratorTopic, payload, e);
        }
    }
}
