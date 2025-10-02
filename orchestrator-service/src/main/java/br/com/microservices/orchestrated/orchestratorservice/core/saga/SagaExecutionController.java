package br.com.microservices.orchestrated.orchestratorservice.core.saga;

import br.com.microservices.orchestrated.orchestratorservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.orchestratorservice.core.dto.Event;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;


import static br.com.microservices.orchestrated.orchestratorservice.core.saga.SagaHandler.*;
import static java.lang.String.format;
import static org.springframework.util.ObjectUtils.isEmpty;

/** SEC **/
@Slf4j
@AllArgsConstructor
@Component
public class SagaExecutionController {

    private static final String SAGA_LOG_ID = "ORDER ID: %s | TRANSACTION ID: %s | EVENT ID: %s";

    /**
     * Método para receber o evento e selecionar o proximo topico a ser enviado pelo orquestrador
     * @param event
     * @return
     */
    public ETopics getNextTopic(Event event){
        if(isEmpty(event.getSource()) || isEmpty(event.getStatus())){
            throw new ValidationException("Source and Status must be Informed!!");
        }
        var topic = findTopicBySourceAndStatus(event);
        logCurrentSaga(event, topic);
        return topic;
    }


    /**
     * Procura o proximo topico comparando com os itens da matrix saga_handler
     * @param event
     * @return
     */
    private ETopics findTopicBySourceAndStatus(Event event){
                        //Utiliza uma stream para transformar em array
        return (ETopics) (Arrays.stream(SAGA_HANDLER)
                //Itera pelas linhas do array e verifica se o evento é igual ao primeiro e segundo item da coluna
                .filter(row -> isEventSourceAndStatusValid(event, row))
                        //Converte o resultado para um objeto pelo terceiro index que é o valor do topico
                        .map(i -> i[TOPIC_INDEX])
                        .findFirst()
                        .orElseThrow(() -> new ValidationException("Topic not found!")));

    }

    /**
     * Valida se o source event e status existem
     * @param event
     * @param row
     * @return
     */
    private boolean isEventSourceAndStatusValid(Event event, Object[] row){
        var source = row[EVENT_SOURCE_INDEX];
        var status = row[SAGA_STATUS_INDEX];
        return event.getSource().equals(source) && event.getStatus().equals(status);
    }

    /**
     * Seleciona o tipo de log success, rollback e fail
     * @param event
     * @param topic
     */
    private void logCurrentSaga(Event event, ETopics topic){
            var sagaId = createSagaId(event);
            var source = event.getSource();
            switch (event.getStatus()){
                case SUCCESS -> log.info("### CURRENT SAGA: {} | SUCCESS | NEXT TOPIC {} | {}",
                        source, topic, sagaId);
                case ROLLBACK_PENDING -> log.info("### CURRENT SAGA: {} | SENDING TO ROLLBACK CURRENT SERVICE | NEXT TOPIC {} | {}",
                        source, topic, sagaId);
                case FAIL -> log.info("### CURRENT SAGA: {} | SENDING TO ROLLBACK PREVIOUS SERVICE | NEXT TOPIC {} | {}",
                        source, topic, sagaId);

            }
    }

    /** Complemento dos logs para sagaId **/
    private String  createSagaId(Event event){
        return format(SAGA_LOG_ID,
                event.getPayload().getId(), event.getTransactionId(), event.getId());
    }
}
