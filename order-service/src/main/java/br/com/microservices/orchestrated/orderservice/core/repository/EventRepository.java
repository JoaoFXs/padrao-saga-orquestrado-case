package br.com.microservices.orchestrated.orderservice.core.repository;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.document.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends MongoRepository<Event, String> {


    /** Método para fazer uma query em todos eventos em ordem descrescente por createdAt **/
    List<Event> findAllByOrderByCreatedAtDesc();

    /** Encontre apenas um item de orderId em ordem decrescente, ou seja, o ultimo criado **/
    Optional<Event> findTop1ByOrderIdOrderByCreatedAtDesc(String orderId);

    /** Encontre apenas um item de transactionId em ordem decrescente, ou seja, o ultimo criado **/
    Optional<Event> findTop1ByTransactionIdOrderByCreatedAtDesc(String transactionId);

}
