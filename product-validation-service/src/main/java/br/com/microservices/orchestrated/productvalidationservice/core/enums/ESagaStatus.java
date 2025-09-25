package br.com.microservices.orchestrated.productvalidationservice.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ESagaStatus {
    SUCCESS,
    ROLLBACK_PENDING,//Quando deu falha mas ainda nao realizou rollback
    FAIL; // Rollback realizado com sucesso


}
