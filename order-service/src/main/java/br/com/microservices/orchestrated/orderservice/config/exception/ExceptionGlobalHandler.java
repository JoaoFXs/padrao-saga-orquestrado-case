package br.com.microservices.orchestrated.orderservice.config.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice//Anotação permitindo um handler global para todos os controllers
public class ExceptionGlobalHandler {


    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?>handleValidationException(ValidationException validationException){

        return ResponseEntity
                .badRequest()
                .body(new ExceptionDetails(HttpStatus.BAD_REQUEST.value(), validationException.getMessage()));
    }


}
