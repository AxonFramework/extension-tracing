/*
 * Copyright (c) 2010-2023. Axon Framework
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

package org.axonframework.extensions.jgroups.integration;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.metrics.Metrics;
import io.jaegertracing.internal.metrics.NoopMetricsFactory;
import io.jaegertracing.internal.reporters.CompositeReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.MetricsFactory;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.spi.Sampler;
import io.opentracing.Tracer;
import io.opentracing.contrib.java.spring.jaeger.starter.JaegerConfigurationProperties;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.extensions.tracing.TracingCommandGateway;
import org.axonframework.extensions.tracing.TracingQueryGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.test.context.ContextConfiguration;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code @EnableConfigurationProperties} and the beans set in {@link DefaultContext} are needed since the
 * {@code opentracing-spring-jaeger-web-starter} is not yet Spring Boot 3 compliant.
 */
class TracingIntegrationTest {

    private ApplicationContextRunner testApplicationContext;

    @BeforeEach
    void setUp() {
        testApplicationContext = new ApplicationContextRunner()
                .withPropertyValues("axon.axonserver.enabled=false")
                .withUserConfiguration(DefaultContext.class);
    }

    @Test
    void queryGatewayIsTracing() {
        testApplicationContext
                .run(context -> {
                    QueryGateway queryGateway = context.getBean(QueryGateway.class);
                    assertNotNull(queryGateway);
                    assertTrue(queryGateway instanceof TracingQueryGateway);
                });
    }

    @Test
    void commandGatewayIsTracing() {
        testApplicationContext
                .run(context -> {
                    CommandGateway commandGateway = context.getBean(CommandGateway.class);
                    assertNotNull(commandGateway);
                    assertTrue(commandGateway instanceof TracingCommandGateway);
                });
    }

    @ContextConfiguration
    @EnableAutoConfiguration
    @EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
    @EnableConfigurationProperties({JaegerConfigurationProperties.class})
    public static class DefaultContext {

        @Bean
        public Tracer tracer(Sampler sampler, Reporter reporter, Metrics metrics,
                             JaegerConfigurationProperties properties) {
            JaegerTracer.Builder builder = (
                    new JaegerTracer.Builder(properties.getServiceName()))
                    .withReporter(reporter)
                    .withSampler(sampler)
                    .withTags(properties.determineTags())
                    .withMetrics(metrics);
            return builder.build();
        }

        @Bean
        public Reporter reporter() {
            List<Reporter> reporters = new LinkedList();
            return new CompositeReporter((Reporter[]) reporters.toArray(new Reporter[reporters.size()]));
        }

        @Bean
        public Metrics metrics(MetricsFactory metricsFactory) {
            return new Metrics(metricsFactory);
        }

        @Bean
        public MetricsFactory metricsFactory() {
            return new NoopMetricsFactory();
        }

        @Bean
        public Sampler sampler() {
            return new ConstSampler(true);
        }
    }
}
