package br.com.microservices.orchestrated.orderservice.core.service;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.repository.EventRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

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


}
