/*
 * Copyright (c) 2010-2019. Axon Framework
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
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.extensions.tracing.OpenTraceDispatchInterceptor;
import org.axonframework.extensions.tracing.OpenTraceHandlerInterceptor;
import org.axonframework.extensions.tracing.TracingCommandGateway;
import org.axonframework.extensions.tracing.TracingProperties;
import org.axonframework.extensions.tracing.TracingProvider;
import org.axonframework.extensions.tracing.TracingQueryGateway;
import org.axonframework.messaging.correlation.CorrelationDataProvider;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.springboot.autoconfig.EventProcessingAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto configure a tracing capabilities.
 *
 * @author Christophe Bouhier
 * @author Steven van Beelen
 * @since 4.0
 */
@Configuration
@AutoConfigureAfter(EventProcessingAutoConfiguration.class)
@ConditionalOnClass(io.opentracing.Tracer.class)
@Import(TracingAutoConfiguration.PropertiesConfiguration.class)
public class TracingAutoConfiguration {

    @Configuration
    public static class PropertiesConfiguration{

        //must be defined in a separate @Configuration to ensure creation order
        @Bean
        @ConfigurationProperties(prefix = "axon.tracing")
        public TracingProperties tracingProperties() {
            return new TracingProperties();
        }
    }

    @Bean
    public OpenTraceDispatchInterceptor traceDispatchInterceptor(Tracer tracer) {
        return new OpenTraceDispatchInterceptor(tracer);
    }

    @Bean
    public OpenTraceHandlerInterceptor traceHandlerInterceptor(Tracer tracer, TracingProperties properties) {
        return new OpenTraceHandlerInterceptor(tracer, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryGateway queryGateway(Tracer tracer,
                                     QueryBus queryBus,
                                     TracingProperties properties,
                                     OpenTraceDispatchInterceptor openTraceDispatchInterceptor,
                                     OpenTraceHandlerInterceptor openTraceHandlerInterceptor) {
        queryBus.registerHandlerInterceptor(openTraceHandlerInterceptor);
        TracingQueryGateway tracingQueryGateway = TracingQueryGateway.builder()
                                                                     .delegateQueryBus(queryBus)
                                                                     .tracer(tracer)
                                                                     .tracingProperties(properties)
                                                                     .build();
        tracingQueryGateway.registerDispatchInterceptor(openTraceDispatchInterceptor);
        return tracingQueryGateway;
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandGateway commandGateway(Tracer tracer,
                                         CommandBus commandBus,
                                         TracingProperties properties,
                                         OpenTraceDispatchInterceptor openTraceDispatchInterceptor,
                                         OpenTraceHandlerInterceptor openTraceHandlerInterceptor) {
        commandBus.registerHandlerInterceptor(openTraceHandlerInterceptor);
        TracingCommandGateway tracingCommandGateway = TracingCommandGateway.builder()
                                                                           .tracer(tracer)
                                                                           .delegateCommandBus(commandBus)
                                                                           .tracingProperties(properties)
                                                                           .build();
        tracingCommandGateway.registerDispatchInterceptor(openTraceDispatchInterceptor);
        return tracingCommandGateway;
    }

    @Bean
    public CorrelationDataProvider tracingProvider(Tracer tracer) {
        return new TracingProvider(tracer);
    }

    @Autowired
    public void configureEventHandler(EventProcessingConfigurer eventProcessingConfigurer,
                                      OpenTraceHandlerInterceptor openTraceHandlerInterceptor) {
        eventProcessingConfigurer.registerDefaultHandlerInterceptor(
                (configuration, name) -> openTraceHandlerInterceptor
        );
    }
}
