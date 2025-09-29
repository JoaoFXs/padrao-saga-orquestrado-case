package br.com.microservices.orchestrated.paymentservice.core.service;

import br.com.microservices.orchestrated.paymentservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.paymentservice.core.dto.Event;
import br.com.microservices.orchestrated.paymentservice.core.dto.History;
import br.com.microservices.orchestrated.paymentservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.paymentservice.core.enums.EPaymentStatus;
import br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.paymentservice.core.model.Payment;
import br.com.microservices.orchestrated.paymentservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.paymentservice.core.repository.PaymentRepository;
import br.com.microservices.orchestrated.paymentservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

    private static final String CURRENT_SOURCE = "PAYMENT_SERVICE";
    private static final Double REDUCE_SUM_VALUE = 0.0;
    private static final Double MIN_AMOUNT_VALUE = 0.1;
    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final PaymentRepository paymentRepository;

    /**
     * Método que realizara o pagamento recebendo um evento de success no consumer kafka
     * @param event
     */
    public void realizePayment(Event event){
        try{
            /** realizePayment util 1 - Verifica se ja existe alguma transação **/
            checkCurrentValidation(event);
            /** realizePayment util 2 - Cria um pagamento pendente **/
            createPendingPayment(event);
            /** Procura pelo orderId e transactionID e valida se o amount é maior que 0.1 **/
            var payment = findByOrderIdAndTransactionId(event);
            validateAmount(payment.getTotalAmount());
            /** Muda o pagamento para success **/
            changePaymentToSuccess(payment);
            /** Lida com sucesso mudando evento **/
            handleSuccess(event);
        } catch (Exception e) {
               log.error("Error Trying to make payment: ", e);
            /** Lida com erro **/
               handleFailCurrentNotExecuted(event, e.getMessage());
        }
        /** Envia evento para producer orchestrate **/
        producer.sendEvent(jsonUtil.toJson(event));
    }

    /**
     *  realizePayment util 1 - Verifica se o evento ja existe uma transação em andamento
     * @param event
     */
    private void checkCurrentValidation(Event event){
        if( paymentRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())){
            throw new RuntimeException("There's another transactionId for this payment.");
        }
    }


    /**
     * realizePayment util 2 - Cria um pagamento pendente e persiste no banco de dados
     * @param event
     */
    private void createPendingPayment(Event event){
        var totalAmount = calculateAmount(event);
        var totalItems = calculateTotalItems(event);
        var payment = Payment
                        .builder()
                        .orderId(event.getOrderId())
                        .transactionId(event.getTransactionId())
                        .totalAmount(totalAmount)
                        .totalItems(totalItems)
                          .build();
        save(payment);
        setEventAmountItems(event, payment);
    }

    /**
     * Calcula o total dos produtos
     *
     * @param event
     * @return
     */
    private double calculateAmount(Event event){

       return event
               .getPayload()
               .getProducts()
               .stream()
               .map(product -> product.getQuantity() * product.getProduct().getUnitValue())
               .reduce(REDUCE_SUM_VALUE, Double::sum);
    }

    /**
     * Calcula o total de itens
     * @param event
     * @return
     */
    private int calculateTotalItems(Event event){
        return event
                .getPayload()
                .getProducts()
                .stream()
                .map(OrderProducts::getQuantity)
                .reduce(REDUCE_SUM_VALUE.intValue(), Integer::sum);
    }

    /**
     * Persiste no banco de dados
     * @param payment
     */
    private void save(Payment payment){
        paymentRepository.save(payment);
    }

    /**
     * Seta o totalamount e totalitems no evento
     * @param event
     * @param payment
     */
    private void setEventAmountItems(Event event, Payment payment){
        event.getPayload().setTotalAmount(payment.getTotalAmount());
        event.getPayload().setTotalItems(payment.getTotalItems());
    }

    /**
     * Valida se o amount é maior que 0.1
     * @param totalAmount
     */
    private void validateAmount(double totalAmount){
        if(totalAmount < MIN_AMOUNT_VALUE){
            throw new ValidationException("The minimum amount available is ".concat(MIN_AMOUNT_VALUE.toString()));
        }
    }

    /**
     * Muda o status do pagamento para success e persiste no banco de dados
     * @param payment
     */
    private void changePaymentToSuccess(Payment payment){
        payment.setStatus(EPaymentStatus.SUCCESS);
        save(payment);
    }

    /**
     * Procura por orderid e transactionid, caso contrario trata exceção
     * @param event
     * @return
     */
    private Payment findByOrderIdAndTransactionId(Event event){
        return paymentRepository.findByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())
                .orElseThrow(() -> new ValidationException("Payment not found by OrderID and TransactionID"));
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
        addHistory(event, "Payment realized with successlly!");
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
        addHistory(event, "Fail to realize payment: ".concat(message));
    }


    /** Blodo de rollback do consumer
     *
     *
     * **/

    /**
     * Metodo para realizar o rollback/ estorno, atualizando event e persistindo o payment como refund. Método chamado no consumer kafka
     * @param event
     */
    public void realizeRefund(Event event){
        event.setStatus(ESagaStatus.FAIL);
        event.setSource(CURRENT_SOURCE);
        try{

           /** Realiza rollback**/
           changePaymentStatusToRefund(event);
           addHistory(event, "Rollback executed for payment!");
       } catch (Exception e) {
           addHistory(event, "Rollback not executed for payment: ".concat(e.getMessage()));
       }

        producer.sendEvent(jsonUtil.toJson(event));
    }

    /**
     * Realiza rollback e persiste no banco de dados como refund
     * @param event
     */
    private void changePaymentStatusToRefund(Event event){
        var payment = findByOrderIdAndTransactionId(event);
        payment.setStatus(EPaymentStatus.REFUND);
        setEventAmountItems(event, payment);
        save(payment);
    }

}
