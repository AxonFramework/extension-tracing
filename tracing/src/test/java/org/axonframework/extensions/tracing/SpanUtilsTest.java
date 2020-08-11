/*
 * Copyright (c) 2010-2020. Axon Framework
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

package org.axonframework.extensions.tracing;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.messaging.GenericMessage;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.GenericQueryMessage;
import org.axonframework.queryhandling.QueryMessage;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class validating the behaviour of the {@link SpanUtils} class.
 *
 * @author Steven van Beelen
 */
class SpanUtilsTest {

    private static final Message<?> TEST_MESSAGE = GenericMessage.asMessage("some-message");
    private static final CommandMessage<Object> TEST_COMMAND = GenericCommandMessage.asCommandMessage("some-command");
    private static final QueryMessage<String, String> TEST_QUERY =
            new GenericQueryMessage<>("some-query", ResponseTypes.instanceOf(String.class));
    private static final EventMessage<String> TEST_EVENT = new GenericEventMessage<>("some-event");
    private static final DomainEventMessage<String> TEST_DOMAIN_EVENT =
            new GenericDomainEventMessage<>("some-event", "aggregateId", 0, "some-payload");

    private MockTracer mockTracer;

    @BeforeEach
    void setUp() {
        mockTracer = new MockTracer();
    }

    @Test
    void testWithTagsForCommandMessage() {
        MockTracer.SpanBuilder spanBuilder = mockTracer.buildSpan("operation-name");

        SpanUtils.withMessageTags(spanBuilder, TEST_COMMAND);

        MockSpan resultSpan = spanBuilder.start();
        Map<String, Object> resultTags = resultSpan.tags();

        assertTrue(resultTags.containsKey("axon.message.id"));
        assertEquals(TEST_COMMAND.getIdentifier(), resultTags.get("axon.message.id"));
        assertTrue(resultTags.containsKey("axon.message.type"));
        assertEquals("CommandMessage", resultTags.get("axon.message.type"));
        assertTrue(resultTags.containsKey("axon.message.payload-type"));
        assertEquals(TEST_COMMAND.getPayloadType().getName(), resultTags.get("axon.message.payload-type"));
        assertTrue(resultTags.containsKey("axon.message.message-name"));
        assertEquals(SpanUtils.messageName(TEST_COMMAND), resultTags.get("axon.message.message-name"));
    }

    @Test
    void testWithTagsForQueryMessage() {
        MockTracer.SpanBuilder spanBuilder = mockTracer.buildSpan("operation-name");

        SpanUtils.withMessageTags(spanBuilder, TEST_QUERY);

        MockSpan resultSpan = spanBuilder.start();
        Map<String, Object> resultTags = resultSpan.tags();

        assertTrue(resultTags.containsKey("axon.message.id"));
        assertEquals(TEST_QUERY.getIdentifier(), resultTags.get("axon.message.id"));
        assertTrue(resultTags.containsKey("axon.message.type"));
        assertEquals("QueryMessage", resultTags.get("axon.message.type"));
        assertTrue(resultTags.containsKey("axon.message.payload-type"));
        assertEquals(TEST_QUERY.getPayloadType().getName(), resultTags.get("axon.message.payload-type"));
        assertTrue(resultTags.containsKey("axon.message.message-name"));
        assertEquals(SpanUtils.messageName(TEST_QUERY), resultTags.get("axon.message.message-name"));
    }

    @Test
    void testWithTagsForDomainEventMessage() {
        MockTracer.SpanBuilder spanBuilder = mockTracer.buildSpan("operation-name");

        SpanUtils.withMessageTags(spanBuilder, TEST_DOMAIN_EVENT);

        MockSpan resultSpan = spanBuilder.start();
        Map<String, Object> resultTags = resultSpan.tags();

        assertTrue(resultTags.containsKey("axon.message.id"));
        assertEquals(TEST_DOMAIN_EVENT.getIdentifier(), resultTags.get("axon.message.id"));
        assertTrue(resultTags.containsKey("axon.message.aggregate-identifier"));
        assertEquals(TEST_DOMAIN_EVENT.getAggregateIdentifier(), resultTags.get("axon.message.aggregate-identifier"));
        assertTrue(resultTags.containsKey("axon.message.type"));
        assertEquals("EventMessage", resultTags.get("axon.message.type"));
        assertTrue(resultTags.containsKey("axon.message.payload-type"));
        assertEquals(TEST_DOMAIN_EVENT.getPayloadType().getName(), resultTags.get("axon.message.payload-type"));
    }

    @Test
    void testWithTagsForEventMessage() {
        MockTracer.SpanBuilder spanBuilder = mockTracer.buildSpan("operation-name");

        SpanUtils.withMessageTags(spanBuilder, TEST_EVENT);

        MockSpan resultSpan = spanBuilder.start();
        Map<String, Object> resultTags = resultSpan.tags();

        assertTrue(resultTags.containsKey("axon.message.id"));
        assertEquals(TEST_EVENT.getIdentifier(), resultTags.get("axon.message.id"));
        assertTrue(resultTags.containsKey("axon.message.type"));
        assertEquals("EventMessage", resultTags.get("axon.message.type"));
        assertTrue(resultTags.containsKey("axon.message.payload-type"));
        assertEquals(TEST_EVENT.getPayloadType().getName(), resultTags.get("axon.message.payload-type"));
    }

    @Test
    void testWithQueryMessageTags() {
        MockTracer.SpanBuilder spanBuilder = mockTracer.buildSpan("operation-name");

        SpanUtils.withQueryMessageTags(spanBuilder, TEST_QUERY, TEST_QUERY.getQueryName());

        MockSpan resultSpan = spanBuilder.start();
        Map<String, Object> resultTags = resultSpan.tags();

        assertTrue(resultTags.containsKey("axon.message.id"));
        assertEquals(TEST_QUERY.getIdentifier(), resultTags.get("axon.message.id"));
        assertTrue(resultTags.containsKey("axon.message.type"));
        assertEquals("QueryMessage", resultTags.get("axon.message.type"));
        assertTrue(resultTags.containsKey("axon.message.payload-type"));
        assertEquals(TEST_QUERY.getPayloadType().getName(), resultTags.get("axon.message.payload-type"));
        assertTrue(resultTags.containsKey("axon.message.message-name"));
        assertEquals(TEST_QUERY.getQueryName(), resultTags.get("axon.message.message-name"));
    }

    @Test
    void testResolveMessageType() {
        assertEquals("Message", SpanUtils.resolveMessageType(TEST_MESSAGE));
        assertEquals("CommandMessage", SpanUtils.resolveMessageType(TEST_COMMAND));
        assertEquals("QueryMessage", SpanUtils.resolveMessageType(TEST_QUERY));
        assertEquals("EventMessage", SpanUtils.resolveMessageType(TEST_EVENT));
        assertEquals("EventMessage", SpanUtils.resolveMessageType(TEST_DOMAIN_EVENT));
    }

    @Test
    void testMessageNameOfMessage() {
        assertEquals(TEST_MESSAGE.getPayloadType().getSimpleName(), SpanUtils.messageName(TEST_MESSAGE));
    }

    @Test
    void testMessageNameOfDefaultCommandMessage() {
        assertEquals(TEST_COMMAND.getPayloadType().getSimpleName(), SpanUtils.messageName(TEST_COMMAND));
    }

    @Test
    void testMessageNameOfCommandMessageWithCustomName() {
        String expectedMessageName = "my-custom-command-name";
        CommandMessage<?> testCommand =
                new GenericCommandMessage<>(GenericMessage.asMessage("some-payload"), expectedMessageName);
        assertEquals(expectedMessageName, SpanUtils.messageName(testCommand));
    }

    @Test
    void testMessageNameOfDefaultQueryMessage() {
        assertEquals(TEST_QUERY.getPayloadType().getSimpleName(), SpanUtils.messageName(TEST_QUERY));
    }

    @Test
    void testMessageNameOfQueryMessageWithCustomName() {
        String expectedMessageName = "my-custom-command-name";
        QueryMessage<String, String> testQuery =
                new GenericQueryMessage<>("some-query", expectedMessageName, ResponseTypes.instanceOf(String.class));
        assertEquals(expectedMessageName, SpanUtils.messageName(testQuery));
    }

    @Test
    void testMessageNameOfEventMessage() {
        assertEquals(TEST_EVENT.getPayloadType().getSimpleName(), SpanUtils.messageName(TEST_EVENT));
    }
}