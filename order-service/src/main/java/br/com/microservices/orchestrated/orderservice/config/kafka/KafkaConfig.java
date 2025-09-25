package br.com.microservices.orchestrated.orderservice.config.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe de configuração central para a integração com o Apache Kafka.
 * Ela é responsável por criar e configurar os "produtores" (quem envia mensagens)
 * e os "consumidores" (quem ouve mensagens) que a aplicação utilizará.
 */
@EnableKafka // (Obrigatório) Ativa a detecção de anotações @KafkaListener pelo Spring. Sem isso, os consumidores declarativos não funcionarão.
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {


    /**
     * Endereço do(s) servidor(es) Kafka. A aplicação usará este endereço para se conectar ao cluster.
     * Motivo: Centraliza o endereço do broker Kafka, facilitando a alteração entre ambientes (desenvolvimento, produção).
     * Uso Futuro: Em um ambiente de produção, aqui teríamos uma lista de endereços separados por vírgula para alta disponibilidade (ex: "kafka1:9092,kafka2:9092").
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServer;

    /**
     * Identificador do grupo de consumidores. Consumidores com o mesmo group-id formam um "time".
     * Motivo: Permite que múltiplas instâncias da sua aplicação trabalhem em conjunto para consumir mensagens de um tópico,
     * distribuindo a carga entre elas. Cada mensagem será entregue a apenas um membro do time.
     */
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Define o comportamento do consumidor quando ele se conecta pela primeira vez ou quando o offset (marcador de leitura) foi perdido.
     * Motivo: Garante um comportamento previsível para novos consumidores.
     * - "earliest": Lê o tópico desde a primeira mensagem disponível.
     * - "latest": (Padrão) Lê apenas as novas mensagens que chegarem após sua conexão.
     * Uso Futuro: A escolha depende da regra de negócio. Se você não pode perder nenhum evento histórico, use "earliest". Se só o que acontece a partir de agora importa, use "latest".
     */
    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    /** Captura Topico **/
    @Value("${spring.kafka.topic.start-saga}")
    private String startSagaTopic;
    /** Captura Topico **/
    @Value("${spring.kafka.topic.notify-ending}")
    private String notifyEndingTopic;

    /** Valores Fixos para partição e replicas para criação de tópicos **/
    private static final Integer PARTITION_COUNT = 1;
    private static final Integer REPLICA_COUNT = 1;

    // ------------------- CONFIGURAÇÕES DO CONSUMER ------------------- //

    /**
     * Cria a "fábrica" de consumidores. O Spring Kafka usará esta fábrica para criar instâncias de consumidores
     * que escutarão as mensagens nos tópicos.
     * Motivo: Abstrai a criação manual de consumidores, delegando essa responsabilidade ao framework.
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerProps());
    }

    /**
     * Define as propriedades específicas para os consumidores.
     * Motivo: Centraliza todas as configurações de como os consumidores devem se comportar.
     */
    private Map<String, Object> consumerProps() {
        var props = new HashMap<String, Object>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer); // Endereço do servidor.
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId); // Define a qual "time" de consumidores esta instância pertence.
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class); // (Desserializador da Chave) Kafka armazena tudo em bytes. Isso ensina o consumidor a "traduzir" a chave da mensagem de bytes de volta para uma String Java.
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class); // (Desserializador do Valor) O mesmo que o anterior, mas para o corpo da mensagem.
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset); // Define a estratégia de leitura inicial do tópico.

        return props;
    }

    // ------------------- CONFIGURAÇÕES DO PRODUCER ------------------- //

    /**
     * Cria a "fábrica" de produtores. Similar ao consumerFactory, mas para quem envia mensagens.
     * Motivo: Padroniza a criação de produtores na aplicação.
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProps());
    }

    /**
     * Define as propriedades específicas para os produtores.
     * Motivo: Centraliza as configurações de como os produtores devem se comportar ao enviar mensagens.
     */
    private Map<String, Object> producerProps() {
        var props = new HashMap<String, Object>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer); // Endereço do servidor.
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class); // (Serializador da Chave) Ensina o produtor a "traduzir" a chave (String Java) para o formato de bytes que o Kafka entende.
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class); // (Serializador do Valor) O mesmo, mas para o corpo da mensagem.
        return props;
    }

    /**
     * Cria o KafkaTemplate, a principal ferramenta do Spring para enviar mensagens.
     * Motivo: Fornece um "helper" de alto nível que simplifica drasticamente o processo de envio.
     * Em vez de lidar com a complexidade do Producer nativo, você simplesmente chama `kafkaTemplate.send(...)`.
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }


    /** Método para criação de tópicos utilizando nome, replica e partições**/
    private NewTopic buildTopic(String name){
        return TopicBuilder
                .name(name)
                .replicas(REPLICA_COUNT)
                .partitions(PARTITION_COUNT)
                .build();
    }

    /** Configuração para iniciar o topico start-saga automaticamente durante a inicialização da aplicação **/
    @Bean
    public NewTopic startSagaTopic(){
        return buildTopic(startSagaTopic);
    }

    /** Configuração para iniciar o topico notify-ending automaticamente durante a inicialização da aplicação **/
    @Bean
    public NewTopic notifyEndingTopic(){
        return buildTopic(notifyEndingTopic);
    }
}