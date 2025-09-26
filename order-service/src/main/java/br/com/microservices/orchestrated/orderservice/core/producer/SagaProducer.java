package br.com.microservices.orchestrated.orderservice.core.producer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


/** Classe criada para trabalhar com produtores**/
@Slf4j//Anotação de logs
@Component
@RequiredArgsConstructor//Utilizado para instanciar apenas o que for necessario de instanciação
public class SagaProducer {
    /** Isso funciona por conta do KafkaConfig realizado**/
    private final KafkaTemplate<String, String> kafkaTemplate;


    /** Topico produtor **/
    @Value("${spring.kafka.topic.start-saga}")
    private String startSagaTopic;

    /** Método para envio de evento **/
    public void sendEvent(String payload){
        try{
            log.info("Sending event to topic {} with data {}", startSagaTopic, payload);
            kafkaTemplate.send(startSagaTopic, payload);
        } catch (Exception e) {
            log.error("Error trying to send data to topic {} with data {}", startSagaTopic, payload, e);
        }
    }
}
