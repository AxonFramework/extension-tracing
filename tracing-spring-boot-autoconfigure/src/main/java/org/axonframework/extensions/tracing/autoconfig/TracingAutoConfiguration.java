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
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ConfigurerModule;
import org.axonframework.extensions.tracing.MessageTagBuilderService;
import org.axonframework.extensions.tracing.OpenTraceDispatchInterceptor;
import org.axonframework.extensions.tracing.OpenTraceHandlerInterceptor;
import org.axonframework.extensions.tracing.TracingCommandGateway;
import org.axonframework.extensions.tracing.TracingProvider;
import org.axonframework.extensions.tracing.TracingQueryGateway;
import org.axonframework.messaging.correlation.CorrelationDataProvider;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.springboot.autoconfig.EventProcessingAutoConfiguration;
import org.axonframework.springboot.autoconfig.InfraConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

/**
 * Auto-configuration defining all required beans to allow a {@link Tracer} to be used on Axon's messaging
 * infrastructure.
 *
 * @author Christophe Bouhier
 * @author Steven van Beelen
 * @author Corrado Musumeci
 * @author Lucas Campos
 * @since 4.0
 */
@AutoConfiguration
@AutoConfigureAfter(EventProcessingAutoConfiguration.class)
@AutoConfigureBefore(InfraConfiguration.class)
@EnableConfigurationProperties(value = {TracingExtensionProperties.class, SpanProperties.class})
@ConditionalOnProperty(value = "axon.extension.tracing.enabled", matchIfMissing = true)
public class TracingAutoConfiguration {

    @Bean
    public OpenTraceDispatchInterceptor traceDispatchInterceptor(Tracer tracer) {
        return new OpenTraceDispatchInterceptor(tracer);
    }

    @Bean
    public OpenTraceHandlerInterceptor traceHandlerInterceptor(Tracer tracer,
                                                               MessageTagBuilderService messageTagBuilderService) {
        return OpenTraceHandlerInterceptor.builder()
                                          .tracer(tracer)
                                          .messageTagBuilderService(messageTagBuilderService)
                                          .build();
    }

    @Lazy
    @Bean
    @ConditionalOnMissingBean
    public QueryGateway tracingQueryGateway(Tracer tracer,
                                            QueryBus queryBus,
                                            OpenTraceDispatchInterceptor openTraceDispatchInterceptor,
                                            OpenTraceHandlerInterceptor openTraceHandlerInterceptor,
                                            MessageTagBuilderService messageTagBuilderService) {
        queryBus.registerHandlerInterceptor(openTraceHandlerInterceptor);
        TracingQueryGateway tracingQueryGateway = TracingQueryGateway.builder()
                                                                     .delegateQueryBus(queryBus)
                                                                     .tracer(tracer)
                                                                     .messageTagBuilderService(messageTagBuilderService)
                                                                     .build();
        tracingQueryGateway.registerDispatchInterceptor(openTraceDispatchInterceptor);
        return tracingQueryGateway;
    }

    @Lazy
    @Bean
    @ConditionalOnMissingBean
    public CommandGateway tracingCommandGateway(Tracer tracer,
                                                CommandBus commandBus,
                                                OpenTraceDispatchInterceptor openTraceDispatchInterceptor,
                                                OpenTraceHandlerInterceptor openTraceHandlerInterceptor,
                                                MessageTagBuilderService messageTagBuilderService) {
        commandBus.registerHandlerInterceptor(openTraceHandlerInterceptor);
        TracingCommandGateway tracingCommandGateway =
                TracingCommandGateway.builder()
                                     .tracer(tracer)
                                     .delegateCommandBus(commandBus)
                                     .messageTagBuilderService(messageTagBuilderService)
                                     .build();
        tracingCommandGateway.registerDispatchInterceptor(openTraceDispatchInterceptor);
        return tracingCommandGateway;
    }

    @Bean
    public CorrelationDataProvider tracingProvider(Tracer tracer) {
        return new TracingProvider(tracer);
    }

    @Bean
    public ConfigurerModule configureTracingInterceptor(OpenTraceHandlerInterceptor openTraceHandlerInterceptor) {
        return configurer -> configurer.eventProcessing().registerDefaultHandlerInterceptor(
                (configuration, name) -> openTraceHandlerInterceptor
        );
    }

    @Bean
    public MessageTagBuilderService spanBuilderService(SpanProperties spanProperties) {
        return MessageTagBuilderService.builder()
                                       .commandMessageTags(spanProperties.getCommandTags())
                                       .eventMessageTags(spanProperties.getEventTags())
                                       .queryMessageTags(spanProperties.getQueryTags())
                                       .build();
    }
}
