package org.axonframework.extensions.tracing.autoconfig;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;
import org.axonframework.extensions.tracing.TracingCommandGateway;
import org.axonframework.extensions.tracing.TracingQueryGateway;
import org.axonframework.queryhandling.DefaultQueryGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.springboot.autoconfig.AxonServerAutoConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@EnableAutoConfiguration(exclude = {
        JmxAutoConfiguration.class,
        WebClientAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        AxonServerAutoConfiguration.class
})
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "axon.extension.tracing.enabled=false",
        "opentracing.jaeger.enabled=true"})
class AxonAutoConfigurationWithoutTracingTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private QueryGateway queryGateway;

    @Autowired
    private CommandGateway commandGateway;

    @Test
    void testContextInitialization() {
        assertNotNull(applicationContext);
        assertFalse(queryGateway instanceof TracingQueryGateway);
        assertFalse(commandGateway instanceof TracingCommandGateway);
        assertTrue(queryGateway instanceof DefaultQueryGateway);
        assertTrue(commandGateway instanceof DefaultCommandGateway);
    }
}