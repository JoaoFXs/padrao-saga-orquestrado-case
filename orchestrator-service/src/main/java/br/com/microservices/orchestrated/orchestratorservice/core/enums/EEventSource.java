package br.com.microservices.orchestrated.orchestratorservice.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


/** ENUM para que o orchestrador saiba qual serviço esta sendo utilizado **/
@Getter

@AllArgsConstructor
public enum EEventSource {
    ORCHESTRATOR,
    PRODUCT_VALIDATION_SERVICE,
    PAYMENT_SERVICE,
    INVENTORY_SERVICE
}
