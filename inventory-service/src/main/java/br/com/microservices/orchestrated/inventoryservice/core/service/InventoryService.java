package br.com.microservices.orchestrated.inventoryservice.core.service;

import br.com.microservices.orchestrated.inventoryservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.inventoryservice.core.dto.Event;
import br.com.microservices.orchestrated.inventoryservice.core.dto.History;
import br.com.microservices.orchestrated.inventoryservice.core.dto.Order;
import br.com.microservices.orchestrated.inventoryservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.inventoryservice.core.model.Inventory;
import br.com.microservices.orchestrated.inventoryservice.core.model.OrderInventory;
import br.com.microservices.orchestrated.inventoryservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.inventoryservice.core.repository.InventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.repository.OrderInventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class InventoryService {
    private static final String CURRENT_SOURCE = "INVENTORY_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final InventoryRepository inventoryRepository;
    private final OrderInventoryRepository orderInventoryRepository;


    /**
     * Método para atualizar inventario, enviar evento para orchestrate ou tratar caso ocorrer algum erro
     * @param event
     */
    public void updateInventory(Event event){

        try{
            /** Valida se há uma transação existente - idempotencia**/
            checkCurrentValidation(event);
            /** Cria OrderInventory utilizando o evento **/
            createOrderInventory(event);
            /** Atualiza inventario utilizando o evento **/
            updateInventory(event.getPayload());
            handleSuccess(event);
        } catch (Exception e) {
            log.error("Error trying to update inventory: ", e);
            handleFailCurrentNotExecuted(event, e.getMessage());
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }

    /**
     * Método para realizar validação de transactionid e orderid no produto
     * @param event
     */
    private void checkCurrentValidation(Event event){
        /** Validação 1 **/
        if(orderInventoryRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())){
            throw new ValidationException("There's another transactionId for this inventory.");
        }

    }

    /**
     *Método para criar uma novar OrderInventory, primeiro encontra o inventario pelo codigo do produto e trata se for necessario, em seguida cria a nova order e
     * persiste no banco de dados
     * @param event
     */
    private void createOrderInventory(Event event){
        event
             .getPayload()
             .getProducts()
             .forEach(product ->{
                    /** Procura inventario atraves do codigo do produto **/
                    var inventory = findInventoryByProductCode(product.getProduct().getCode());
                    /** cria novo OrderIventoru **/
                    var orderInventory = createOrderInventory(event, product, inventory);
                    orderInventoryRepository.save(orderInventory);
             });
    }
    /** Procura inventario se existir **/
    private Inventory findInventoryByProductCode(String productCode){
        return inventoryRepository
                .findByProductCode(productCode)
                .orElseThrow(() -> new ValidationException("Inventory not found by informed product code"));
    }

    /**
     * Método para criar uma nova OrderInventory, registra quantidade antiga do inventario,
     * a quantidade do pedido e a nova quantidade, subtraindo disponivel pela quantidad
     * @param event
     * @param product
     * @param inventory
     * @return
     */
    private OrderInventory createOrderInventory(Event event, OrderProducts product, Inventory inventory){
        return OrderInventory
                .builder()
                .inventory(inventory)
                .oldQuantity(inventory.getAvailable())
                .orderQuantity(product.getQuantity())
                .newQuantity(inventory.getAvailable() - product.getQuantity())
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .build();
    }


    /**
     *   Atualiza inventario com valores novos. Passa por cada produto, veriifca se existe no inventario, checa se o orderQuantity não é maior que o disponivel e
     *   atualiza o inventario.
     * @param order
     */
    private void updateInventory(Order order){
        order.getProducts().forEach(
                product -> {
                    var inventory = findInventoryByProductCode(product.getProduct().getCode());
                    checkInventory(inventory.getAvailable(), product.getQuantity());
                    inventory.setAvailable(inventory.getAvailable() - product.getQuantity());
                    inventoryRepository.save(inventory);
                }
        );
    }

    /**
     * Valida se o orderQuantity não é maior que o disponivel
     * @param available
     * @param orderQuantity
     */
    private void checkInventory (int available, int orderQuantity){
        if (orderQuantity > available){
            throw new ValidationException("Product is out of stock!");
        }
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
        addHistory(event, "Inventory updated successfully!");
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
        addHistory(event, "Fail to updated inventory: ".concat(message));
    }

    /**
     * Método para tratar rollbackEvents, utilizado direto no listener do kafka consumer. Adiciona falha como status, e realiza o rollback.
     *
     * @param event
     */
    public void rollbackInventory(Event event){
        event.setStatus(ESagaStatus.FAIL);
        event.setSource(CURRENT_SOURCE);
        try{
            /** Faz rollback no inventario **/
            returnInventoryToPreviousValues(event);
            addHistory(event, "Rollback executed for inventory!");
        } catch (Exception e) {
            addHistory(event, "Rollback not executed for inventory".concat(e.getMessage()));
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }

    /**
     * Procura no OrderInventory os registros utilizando o id e transactionid, se encontrar, faz a atualização, pegando o valor antigo e inserindo em available.
     * Em seguida persiste no banco de dados.
     * @param event
     */
    private void returnInventoryToPreviousValues(Event event){
        orderInventoryRepository.findByOrderIdAndTransactionId(event.getPayload().getId(), event.getTransactionId())
                .forEach(orderInventory -> {
                    var inventory = orderInventory.getInventory();
                    inventory.setAvailable(orderInventory.getOldQuantity());
                    inventoryRepository.save(inventory);
                    log.info("Restored inventory for order {} from {} to {}", event.getPayload().getId(), orderInventory.getNewQuantity(), inventory.getAvailable());
                });
    }

}
