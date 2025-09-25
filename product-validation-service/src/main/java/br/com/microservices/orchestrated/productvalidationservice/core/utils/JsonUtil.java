package br.com.microservices.orchestrated.productvalidationservice.core.utils;




import br.com.microservices.orchestrated.productvalidationservice.core.dto.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/** Classe utilitaria feita para converter objetos de eventos em json e em string **/
@Component
@AllArgsConstructor
public class JsonUtil {

    private final ObjectMapper objectMapper;

    /**
     * Método utilitario para converter um objeto em json string
     * **/
    public String toJson(Object object){
        try{
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            return "";
        }
    }
    /**
     * Método utilitario para converter uma string em objeto
     * **/
    public Event toEvent(String json){
        try{
            return objectMapper.readValue(json, Event.class);
        } catch (Exception e) {
            return null;
        }
    }


}
