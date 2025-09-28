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

    private static final String TRANSACTION_ID_PATTERN = "%s_%s";

    private final JsonUtil jsonUtil;
    private final SagaProducer producer;
    private final OrderRepository orderRepository;
    private final EventService eventService;
    public Order createOrder(OrderRequest request){
        var order = Order
                    .builder()
                    .products(request.getProducts())
                    .createdAt(LocalDateTime.now())
                    .transactionId(
                        String.format(TRANSACTION_ID_PATTERN, Instant.now().toEpochMilli(), UUID.randomUUID())
                    )
                    .build();
        orderRepository.save(order);
        producer.sendEvent(jsonUtil.toJson(createPayload(order)));
        return order;
    }

   private Event createPayload(Order order){
        var event = Event
                    .builder()
                    .payload(order)
                    .orderId(order.getId())
                    .createdAt(LocalDateTime.now())
                    .build();

        eventService.save(event);
        return event;
   }
}
