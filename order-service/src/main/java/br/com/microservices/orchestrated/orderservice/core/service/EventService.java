package br.com.microservices.orchestrated.orderservice.core.service;

import br.com.microservices.orchestrated.orderservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.dto.EventFilters;
import br.com.microservices.orchestrated.orderservice.core.repository.EventRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


import static org.springframework.util.ObjectUtils.isEmpty;
@Service
@AllArgsConstructor
public class EventService {

    private final EventRepository repository;

    /**
     *  Método responsavel por salvar event em collection do mongodb
     * @param event Evento a ser salvo
     * @return evento salvo retornado
     * **/
    public Event save(Event event){
        return repository.save(event);
    }


    /**
     * Método para receber de topico notify-ending do component EventConsumer,
     * e persistir na o document na collection  com a notificação de finalização
     *
     * @param event
     */
    public void notifyEnding(Event event){
        event.setOrderId(event.getOrderId());
        event.setCreatedAt(LocalDateTime.now());
        save(event);
    }


    /**
     * Método para fazer uma query em todos eventos por ordem decrescente
     *
     * @return List<Event>
     */
    public List<Event> findAll(){
        return repository.findAllByOrderByCreatedAtDesc();
    }


    /**
     * Método utilizado no controller event para filtrar os valores de order Id e transaction Id
     * @param filters
     * @return
     */
    public Event findByFilters(EventFilters filters){
        validateEmptyFilters(filters);
        if (!isEmpty(filters.getOrderId())){
            return findByOrderId(filters.getOrderId());
        } else{
            return findByTransactionId(filters.getTransactionId());
        }
    }

    /**
     * Método utilitario para filtrar por orderId
     * @param orderId
     * @return
     */
    private Event findByOrderId(String orderId){
        return repository
                .findTop1ByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ValidationException("Event not found by orderID."));
    }

    /**
     * Método utilitario para filtrar por transactionId
     * @param transactionId
     * @return
     */
    private Event findByTransactionId(String transactionId){
        return repository
                .findTop1ByTransactionIdOrderByCreatedAtDesc(transactionId)
                .orElseThrow(() -> new ValidationException("Event not found by TransactionID."));

    }

    /**
     * Método utilitario para tratar orderId e transactionId vazios
     * @param filters
     */
    private void validateEmptyFilters(EventFilters filters){
        if (isEmpty(filters.getOrderId()) && isEmpty(filters.getTransactionId())){
            throw new ValidationException("OrderID or TransactionID must be informed");
        }
    }

}
