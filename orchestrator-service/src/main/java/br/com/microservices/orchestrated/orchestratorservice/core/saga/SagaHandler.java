package br.com.microservices.orchestrated.orchestratorservice.core.saga;



import static br.com.microservices.orchestrated.orchestratorservice.core.enums.EEventSource.*;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ESagaStatus.*;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics.*;

public final class SagaHandler {

    private SagaHandler(){

    }

    private static final Object [][] SAGA_HANDLER ={
            /**
             * Se recebeu como orchestrator, e status success, envia para PRUDUCT_VALIDATION_SERVICE**/
            {ORCHESTRATOR, SUCCESS, PRODUCT_VALIDATION_SUCCESS},
            /** Se recebeu como orchestrator, e status fail, envia para FINISH_FAIL**/
            {ORCHESTRATOR, FAIL, FINISH_FAIL},

            /** Se orquestrador recebeu PRODUCT_VALIDATION_SERVICE com ROLLBACK_PENDING,
             *  retorna para o topico PRODUCT_VALIDATION_FAIL para realizar um rollback**/
            {PRODUCT_VALIDATION_SERVICE, ROLLBACK_PENDING, PRODUCT_VALIDATION_FAIL},
            /** Se orquestrador recebeu PRODUCT_VALIDATION_SERVICE com FAIL,
             *  retorna para o topico FINISH_FAIL para finalizar o processo**/
            {PRODUCT_VALIDATION_SERVICE, FAIL, FINISH_FAIL},
            /** Se orquestrador recebeu PRODUCT_VALIDATION_SERVICE com SUCCESS,
             *  retorna para o topico PAYMENT_SUCCESS para continuação do processo**/
            {PRODUCT_VALIDATION_SERVICE, SUCCESS, PAYMENT_SUCCESS},


            /** Se orquestrador recebeu PAYMENT_SERVICE com ROLLBACK_PENDING,
             *  retorna para o topico PAYMENT_FAIL para realizar um rollback**/
            {PAYMENT_SERVICE, ROLLBACK_PENDING, PAYMENT_FAIL},
            /** Se orquestrador recebeu PAYMENT_SERVICE com FAIL,
             *  retorna para o topico PRODUCT_VALIDATION_FAIL para realizar o rollback do PRODUCT_VALIDATION_SERVICE**/
            {PAYMENT_SERVICE, FAIL, PRODUCT_VALIDATION_FAIL},
            /** Se orquestrador recebeu PAYMENT_SERVICE com SUCCESS,
             *  retorna para o topico INVENTORY_SUCCESS para continuação do processo**/
            {PAYMENT_SERVICE, SUCCESS, INVENTORY_SUCCESS},


            /** Se orquestrador recebeu INVENTORY_SERVICE com ROLLBACK_PENDING,
             *  retorna para o topico INVENTORY_FAIL para realizar um rollback**/
            {INVENTORY_SERVICE, ROLLBACK_PENDING, INVENTORY_FAIL},
            /** Se orquestrador recebeu INVENTORY_SERVICE com FAIL,
             *  retorna para o topico PAYMENT_FAIL para realizar o rollback do PAYMENT_SERVICE**/
            {INVENTORY_SERVICE, FAIL, PAYMENT_FAIL},
            /** Se orquestrador recebeu INVENTORY_SERVICE com SUCCESS,
             *  retorna para o topico FINISH_SUCCESS para finalização do processo**/
            {INVENTORY_SERVICE, SUCCESS, FINISH_SUCCESS}

    };

    /** Coluna do EventSource **/
    public static final int EVENT_SOURCE_INDEX = 0;
    /** Coluna do status da Saga **/
    public static final int SAGA_STATUS_INDEX = 1;
    /** Coluna do Topico **/
    public static final int TOPIC_INDEX = 2;



}
