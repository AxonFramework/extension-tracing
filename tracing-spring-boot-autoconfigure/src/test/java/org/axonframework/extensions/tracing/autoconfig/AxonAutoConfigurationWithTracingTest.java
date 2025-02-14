/*
 * Copyright (c) 2010-2025. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class validating the auto-configuration of distributed tracing specific infrastructure components.
 *
 * @author Christophe Bouhier
 */
@SuppressWarnings("SpringJavaAutowiredMembersInspection")

@ExtendWith(SpringExtension.class)
class AxonAutoConfigurationWithTracingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("axon.axonserver.enabled=false")
            .withConfiguration(AutoConfigurations.of(Config.class));

    @Test
    void testContextInitialization() {
        contextRunner.run(context -> {
            assertInstanceOf(TracingCommandGateway.class, context.getBean(CommandGateway.class));
            assertInstanceOf(TracingQueryGateway.class, context.getBean(QueryGateway.class));
            assertNotNull(context.getBean(Tracer.class));
            assertNotNull(context.getBean(OpenTraceDispatchInterceptor.class));
            assertNotNull(context.getBean(OpenTraceHandlerInterceptor.class));
            assertNotNull(context.getBean(TracingProvider.class));
            assertNotNull(context.getBean(MessageTagBuilderService.class));
        });
    }

    @EnableAutoConfiguration(exclude = {
            JmxAutoConfiguration.class,
            WebClientAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataSourceAutoConfiguration.class
    })
    public static class Config {

    }
}