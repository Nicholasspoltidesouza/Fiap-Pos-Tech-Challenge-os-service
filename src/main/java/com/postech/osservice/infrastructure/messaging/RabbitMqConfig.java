package com.postech.osservice.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RabbitMqConfig {

    public static final String QUEUE_ORCAMENTO_GERADO = "os.orcamento.gerado";
    public static final String QUEUE_ORCAMENTO_APROVADO = "os.orcamento.aprovado";
    public static final String QUEUE_ORCAMENTO_REJEITADO = "os.orcamento.rejeitado";
    public static final String QUEUE_PAGAMENTO_APROVADO = "os.pagamento.aprovado";
    public static final String QUEUE_PAGAMENTO_FALHOU = "os.pagamento.falhou";
    public static final String QUEUE_EXECUCAO_INICIADA = "os.execucao.iniciada";
    public static final String QUEUE_EXECUCAO_FINALIZADA = "os.execucao.finalizada";

    @Bean
    TopicExchange oficinaEventsExchange(@Value("${oficina.events.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    MessageConverter jacksonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter);
        return template;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jacksonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter);
        return factory;
    }

    @Bean
    Queue orcamentoGeradoQueue() {
        return new Queue(QUEUE_ORCAMENTO_GERADO, true);
    }

    @Bean
    Queue orcamentoAprovadoQueue() {
        return new Queue(QUEUE_ORCAMENTO_APROVADO, true);
    }

    @Bean
    Queue orcamentoRejeitadoQueue() {
        return new Queue(QUEUE_ORCAMENTO_REJEITADO, true);
    }

    @Bean
    Queue pagamentoAprovadoQueue() {
        return new Queue(QUEUE_PAGAMENTO_APROVADO, true);
    }

    @Bean
    Queue pagamentoFalhouQueue() {
        return new Queue(QUEUE_PAGAMENTO_FALHOU, true);
    }

    @Bean
    Queue execucaoIniciadaQueue() {
        return new Queue(QUEUE_EXECUCAO_INICIADA, true);
    }

    @Bean
    Queue execucaoFinalizadaQueue() {
        return new Queue(QUEUE_EXECUCAO_FINALIZADA, true);
    }

    @Bean
    Binding bindOrcamentoGerado(Queue orcamentoGeradoQueue, TopicExchange oficinaEventsExchange) {
        return BindingBuilder.bind(orcamentoGeradoQueue).to(oficinaEventsExchange).with("orcamento.gerado");
    }

    @Bean
    Binding bindOrcamentoAprovado(Queue orcamentoAprovadoQueue, TopicExchange oficinaEventsExchange) {
        return BindingBuilder.bind(orcamentoAprovadoQueue).to(oficinaEventsExchange).with("orcamento.aprovado");
    }

    @Bean
    Binding bindOrcamentoRejeitado(Queue orcamentoRejeitadoQueue, TopicExchange oficinaEventsExchange) {
        return BindingBuilder.bind(orcamentoRejeitadoQueue).to(oficinaEventsExchange).with("orcamento.rejeitado");
    }

    @Bean
    Binding bindPagamentoAprovado(Queue pagamentoAprovadoQueue, TopicExchange oficinaEventsExchange) {
        return BindingBuilder.bind(pagamentoAprovadoQueue).to(oficinaEventsExchange).with("pagamento.aprovado");
    }

    @Bean
    Binding bindPagamentoFalhou(Queue pagamentoFalhouQueue, TopicExchange oficinaEventsExchange) {
        return BindingBuilder.bind(pagamentoFalhouQueue).to(oficinaEventsExchange).with("pagamento.falhou");
    }

    @Bean
    Binding bindExecucaoIniciada(Queue execucaoIniciadaQueue, TopicExchange oficinaEventsExchange) {
        return BindingBuilder.bind(execucaoIniciadaQueue).to(oficinaEventsExchange).with("execucao.iniciada");
    }

    @Bean
    Binding bindExecucaoFinalizada(Queue execucaoFinalizadaQueue, TopicExchange oficinaEventsExchange) {
        return BindingBuilder.bind(execucaoFinalizadaQueue).to(oficinaEventsExchange).with("execucao.finalizada");
    }
}
