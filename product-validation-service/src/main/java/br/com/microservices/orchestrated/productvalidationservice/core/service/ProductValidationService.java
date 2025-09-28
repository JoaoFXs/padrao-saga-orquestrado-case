package br.com.microservices.orchestrated.productvalidationservice.core.service;


import br.com.microservices.orchestrated.productvalidationservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.Event;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.History;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.productvalidationservice.core.model.Validation;
import br.com.microservices.orchestrated.productvalidationservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ProductRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ValidationRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static org.springframework.util.ObjectUtils.isEmpty;

@Service
@AllArgsConstructor
@Slf4j
public class ProductValidationService {

    /** CURRENT_SOURCE - utilizado para informar qual microsserviço está a realizar operações **/
    private static final String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";

    /** Instanciando variaveis **/
    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final ProductRepository productRepository;
    private final ValidationRepository validationRepository;

    /**
     * Método para realizar validações, enviar evento para orchestrate ou tratar caso ocorrer algum erro
     * @param event
     */
    public void validateExistingProducts(Event event){
        try{
            checkCurrentValidation(event);
            createValidation(event, true);
            handleSuccess(event);
        } catch (Exception e) {
            log.error("Error trying to validate products: ", e);
            handleFailCurrentNotExecuted(event, e.getMessage());
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }

    /**
     * ##################################
     * Bloco de código para validações
     * ##################################
     * **/

    /**
     * Método para realizar validação de transactionid e orderid no produto, além de validar se codigo do produto existe no banco de dados
     * @param event
     */
    private void checkCurrentValidation(Event event){
        /** Validação 1 **/
        validateProductsInformed(event);
        if(validationRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())){
            throw new ValidationException("There's another transactionId for this validation.");
        }

        event.getPayload().getProducts().forEach( product ->{
                    /** Validação 2 **/
                    validateProductInformed(product);
                     /** Validação 3 **/
                    validateExistingProduct(product.getProduct().getCode());
                }
        );
    }

    /** Validação 1 - Método para validar se payload e produtos existem,
     *                além de validar se o id e transactionid foram informados
     * @param event
     */
    private void validateProductsInformed(Event event){
        if(isEmpty(event.getPayload()) || isEmpty(event.getPayload().getProducts())){
            throw new ValidationException("Products list is empty!");
        }

        if(isEmpty(event.getPayload().getId()) || isEmpty(event.getPayload().getTransactionId())){
            throw new ValidationException("OrderID and TransactionID must be informed!");
        }
    }

    /** Validação 2 - Método para validar se produto e codigo do produto existem
     *
     * @param product
     */
    private void validateProductInformed(OrderProducts product){
           if (isEmpty(product.getProduct() )|| isEmpty(product.getProduct().getCode())){
                throw new ValidationException("Product must be informed!");
           }
    }

    /** Validação 3 - Método para validar se código do produto é valido na lista de codigos de produtos permitidos
     *
     * @param code
     */
    private void validateExistingProduct(String code){
        boolean validation = productRepository.existsByCode(code);

        if(!validation){
            throw new ValidationException("Product code: " + code + " don't exists in database");
        }
    }

    /**
     * Método que só ocorrerá se todas as validações forem positivas. Nele, é montado uma validação nova, informando status de sucesso e persistindo no DB
     * @param event
     * @param success
     */
    private void createValidation(Event event, boolean success){
        var validation = Validation
                        .builder()
                        .orderId(event.getPayload().getId())
                        .transactionId(event.getTransactionId())
                        .success(success)
                        .build();
        validationRepository.save(validation);
    }

    /**
     * Método para lidar com sucesso, salvando status como success e atribuindo source com current_source.
     * Por mim chama addHistory para adicionar um historico ao evento
     * @param event
     */
    private void handleSuccess(Event event){
        event.setStatus(ESagaStatus.SUCCESS);
        event.setSource(CURRENT_SOURCE);
        /** Utilitario handleSuccess 1 **/
        addHistory(event, "Products are validated successfully!");
    }

    /**
     * Utilitario handleSuccess 1 - Cria historico novo e adiciona na lista de historicos
     *
     * @param event
     * @param message
     */
    private void addHistory(Event event, String message){
        var history = History.builder()
                        .source(event.getSource())
                        .status(event.getStatus())
                        .message(message)
                        .createdAt(LocalDateTime.now())
                        .build();
        event.addToHistory(history);
    }


    /**
     * Método responsavel por lidar com rollback pendings e enviar para o orchestrator
     * @param event
     * @param message
     */
    private void handleFailCurrentNotExecuted(Event event, String message){
        event.setStatus(ESagaStatus.ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to validate products: ".concat(message));
    }

    /**
     * Método para tratar rollbackEvents, utilizado direto no listener do kafka consumer. Adiciona falha como status, e realiza o rollback.
     *
     * @param event
     */
    public void rollbackEvent(Event event){
        /** Utilitario 1 rollbackEvent **/
        changeValidationToFail(event);
        event.setStatus(ESagaStatus.FAIL);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Rollback executed on product validation!");
        producer.sendEvent(jsonUtil.toJson(event));
    }

    /**
     * Utilitario 1 rollbackEvent: Utilizado para persistir o rollback, transformando o success como falso e salvando a validação.
     * É realizado uma verificação se existe o id e transactionid no banco de dados, se nao existir, salva mesmo para manter registrado.
     * @param event
     */
    private void changeValidationToFail(Event event){
        validationRepository.findByOrderIdAndTransactionId(event.getPayload().getId(), event.getTransactionId())
                .ifPresentOrElse(validation -> {
                    validation.setSuccess(false);
                    validationRepository.save(validation);
                },
                () ->{
                    createValidation(event, false);
                })
                ;
    }

}
