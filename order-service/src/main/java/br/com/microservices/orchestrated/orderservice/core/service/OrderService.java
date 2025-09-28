package br.com.microservices.orchestrated.orderservice.core.service;


import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.document.Order;
import br.com.microservices.orchestrated.orderservice.core.dto.OrderRequest;
import br.com.microservices.orchestrated.orderservice.core.producer.SagaProducer;
import br.com.microservices.orchestrated.orderservice.core.repository.EventRepository;
import br.com.microservices.orchestrated.orderservice.core.repository.OrderRepository;
import br.com.microservices.orchestrated.orderservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OrderService {
    /** Pattern do transactionId **/
    private static final String TRANSACTION_ID_PATTERN = "%s_%s";

    /** Conversor objeto e string **/
    private final JsonUtil jsonUtil;
    /** Component para envio de saga para produtor start-saga **/
    private final SagaProducer producer;

    /** Repositorios  **/
    private final OrderRepository orderRepository;
    private final EventService eventService;

    /**
     * Método para geração de Order, salvamento em repository e criação + envio de evento  para producer
     * @param request OrderRequest gerado
     * @return Order gerado
     * **/
    public Order createOrder(OrderRequest request){
        var order = Order
                    .builder()
                    .products(request.getProducts())
                    .createdAt(LocalDateTime.now())
                    .transactionId(
                            /** Geração de transactionId unico **/
                        String.format(TRANSACTION_ID_PATTERN, Instant.now().toEpochMilli(), UUID.randomUUID())
                    )
                    .build();
        orderRepository.save(order);
        /** Converte em string/json antes do envio **/
        producer.sendEvent(jsonUtil.toJson(createPayload(order)));
        return order;
    }


    /**
     *  Método para criação de payload de evento e persistencia em mongodb.
     *
     * @param order Order gerado em createOrder
     * @return event gerado
     */
    private Event createPayload(Order order){
        var event = Event
                    .builder()
                .transactionId(order.getTransactionId())
                    .payload(order)
                    .orderId(order.getId())
                    .createdAt(LocalDateTime.now())
                    .build();

        eventService.save(event);
        return event;
   }
}
