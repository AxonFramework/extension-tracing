package org.axonframework.extensions.tracing.autoconfig;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.extensions.tracing.OpenTraceDispatchInterceptor;
import org.axonframework.extensions.tracing.OpenTraceHandlerInterceptor;
import org.axonframework.extensions.tracing.TracingCommandGateway;
import org.axonframework.extensions.tracing.TracingQueryGateway;
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
class AxonAutoConfigurationWithTracingTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private QueryGateway queryGateway;

    @Autowired
    private OpenTraceHandlerInterceptor openTraceHandlerInterceptor;

    @Autowired
    private OpenTraceDispatchInterceptor openTraceDispatchInterceptor;

    @Autowired
    private CommandGateway commandGateway;

    @Test
    void testContextInitialization() {
        assertNotNull(applicationContext);
        assertNotNull(openTraceDispatchInterceptor);
        assertNotNull(openTraceHandlerInterceptor);
        assertTrue(queryGateway instanceof TracingQueryGateway);
        assertTrue(commandGateway instanceof TracingCommandGateway);
    }
}