package org.axonframework.extensions.tracing.autoconfig;

import io.opentracing.Tracer;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.extensions.tracing.MessageTagBuilderService;
import org.axonframework.extensions.tracing.OpenTraceDispatchInterceptor;
import org.axonframework.extensions.tracing.OpenTraceHandlerInterceptor;
import org.axonframework.extensions.tracing.TracingCommandGateway;
import org.axonframework.extensions.tracing.TracingProvider;
import org.axonframework.extensions.tracing.TracingQueryGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class validating the auto configuration of distributed tracing specific infrastructure components.
 *
 * @author Christophe Bouhier
 */
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@EnableAutoConfiguration(exclude = {
        JmxAutoConfiguration.class,
        WebClientAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataSourceAutoConfiguration.class
})
@ExtendWith(SpringExtension.class)
class AxonAutoConfigurationWithTracingTest {

    @Autowired
    private CommandGateway commandGateway;
    @Autowired
    private QueryGateway queryGateway;

    @Autowired
    private OpenTraceHandlerInterceptor openTraceHandlerInterceptor;
    @Autowired
    private OpenTraceDispatchInterceptor openTraceDispatchInterceptor;
    @Autowired
    private TracingProvider tracingProvider;
    @Autowired
    private MessageTagBuilderService messageTagBuilderService;
    @Autowired
    private Tracer tracer;

    @Test
    void testContextInitialization() {
        assertTrue(commandGateway instanceof TracingCommandGateway);
        assertTrue(queryGateway instanceof TracingQueryGateway);
        assertNotNull(tracer);
        assertNotNull(openTraceDispatchInterceptor);
        assertNotNull(openTraceHandlerInterceptor);
        assertNotNull(tracingProvider);
        assertNotNull(messageTagBuilderService);
    }
}