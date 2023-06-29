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

package org.axonframework.extensions.tracing.autoconfig;

import io.opentracing.Tracer;
import org.axonframework.extensions.tracing.MessageTag;
import org.axonframework.extensions.tracing.MessageTagBuilderService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class validating the configuration of {@link org.axonframework.extensions.tracing.MessageTag}s through the
 * properties file.
 *
 * @author Corrado Musumeci
 * @author Steven van Beelen
 */
@ExtendWith(SpringExtension.class)
class TracingAutoConfigurationMessageTagTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("axon.axonserver.enabled=false")
            .withConfiguration(AutoConfigurations.of(TracingAutoConfigurationMessageTagTest.Config.class));

    @Test
    void testMessageTagWithCustomConfiguration() {
        contextRunner.withPropertyValues(
                "axon.extension.tracing.span.commandTags=messageId, messageType, messageName",
                "axon.extension.tracing.span.eventTags=messageId, aggregateId, messageName",
                "axon.extension.tracing.span.queryTags=messageId, messageType, payload"
        ).run(context -> {
            List<MessageTag> expectedCommandTags =
                    asList(MessageTag.MESSAGE_ID, MessageTag.MESSAGE_TYPE, MessageTag.MESSAGE_NAME);
            List<MessageTag> expectedEventTags =
                    asList(MessageTag.MESSAGE_ID, MessageTag.AGGREGATE_ID, MessageTag.MESSAGE_NAME);
            List<MessageTag> expectedQueryTags =
                    asList(MessageTag.MESSAGE_ID, MessageTag.MESSAGE_TYPE, MessageTag.PAYLOAD);

            assertNotNull(context.getBeanNamesForType(SpanProperties.class));
            SpanProperties result = context.getBean(SpanProperties.class);
            assertIterableEquals(expectedCommandTags, result.getCommandTags());
            assertIterableEquals(expectedEventTags, result.getEventTags());
            assertIterableEquals(expectedQueryTags, result.getQueryTags());
            assertNotNull(context.getBeanNamesForType(MessageTagBuilderService.class));
        });
    }

    @Test
    void testMessageTagWithoutCustomConfiguration() {
        contextRunner.run(context -> {
            SpanProperties defaultSpanProperties = new SpanProperties();
            List<MessageTag> expectedCommandTags = defaultSpanProperties.getCommandTags();
            List<MessageTag> expectedEventTags = defaultSpanProperties.getEventTags();
            List<MessageTag> expectedQueryTags = defaultSpanProperties.getQueryTags();

            assertNotNull(context.getBeanNamesForType(SpanProperties.class));
            SpanProperties result = context.getBean(SpanProperties.class);
            assertIterableEquals(expectedCommandTags, result.getCommandTags());
            assertIterableEquals(expectedEventTags, result.getEventTags());
            assertIterableEquals(expectedQueryTags, result.getQueryTags());
            assertNotNull(context.getBeanNamesForType(MessageTagBuilderService.class));
        });
    }

    @EnableAutoConfiguration(exclude = {
            JpaRepositoriesAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    public static class Config {

        @Bean
        public Tracer tracer() {
            return mock(Tracer.class);
        }
    }
}