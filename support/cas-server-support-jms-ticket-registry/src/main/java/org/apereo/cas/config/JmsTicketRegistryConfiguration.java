package org.apereo.cas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.StringBean;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.ticket.registry.JmsTicketRegistry;
import org.apereo.cas.ticket.registry.JmsTicketRegistryReceiver;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.CoreTicketUtils;
import org.apereo.cas.util.serialization.AbstractJacksonBackedStringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import javax.jms.ConnectionFactory;

/**
 * This is {@link JmsTicketRegistryConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Configuration("jmsTicketRegistryConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@EnableJms
@Slf4j
public class JmsTicketRegistryConfiguration {
    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Bean
    public StringBean messageQueueTicketRegistryIdentifier() {
        return new StringBean();
    }

    @Bean
    public JmsTicketRegistryReceiver messageQueueTicketRegistryReceiver() {
        return new JmsTicketRegistryReceiver(ticketRegistry(), messageQueueTicketRegistryIdentifier());
    }

    @Bean
    public TicketRegistry ticketRegistry() {
        final var jms = casProperties.getTicket().getRegistry().getJms();
        final var cipher = CoreTicketUtils.newTicketRegistryCipherExecutor(jms.getCrypto(), "jms");
        return new JmsTicketRegistry(this.jmsTemplate, messageQueueTicketRegistryIdentifier(), cipher);
    }

    @Autowired
    @Bean
    public JmsListenerContainerFactory<?> messageQueueTicketRegistryFactory(final ConnectionFactory connectionFactory,
                                                                            final DefaultJmsListenerContainerFactoryConfigurer configurer) {
        final var factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        return factory;
    }

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        final var converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");

        new AbstractJacksonBackedStringSerializer<>() {
            private static final long serialVersionUID = 1466569521275630254L;

            @Override
            protected Class getTypeToSerialize() {
                return Object.class;
            }

            @Override
            protected ObjectMapper initializeObjectMapper() {
                final var mapper = super.initializeObjectMapper();
                converter.setObjectMapper(mapper);
                return mapper;
            }
        };

        return converter;
    }
}
