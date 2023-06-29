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

package org.axonframework.extensions.tracing;

import io.opentracing.Tracer;
import org.axonframework.messaging.GenericMessage;
import org.axonframework.messaging.Message;
import org.junit.jupiter.api.*;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class validating the {@link MessageTagBuilder} functional interface.
 *
 * @author Corrado Musumeci
 * @author Steven van Beelen
 */
class MessageTagBuilderTest {

    private Tracer.SpanBuilder testSpanBuilder;
    private final Message<?> testMessage = GenericMessage.asMessage("our-awesome-message");

    @BeforeEach
    void setUp() {
        testSpanBuilder = mock(Tracer.SpanBuilder.class);
    }

    @Test
    void testWithInvokesBothMessageTagBuilderInstances() {
        AtomicBoolean invokedFirst = new AtomicBoolean(false);
        MessageTagBuilder first = (builder, message) -> {
            invokedFirst.set(true);
            return builder;
        };
        AtomicBoolean invokedSecond = new AtomicBoolean(false);
        MessageTagBuilder second = (builder, message) -> {
            invokedSecond.set(true);
            return builder;
        };

        MessageTagBuilder testSubject = first.with(second);

        testSubject.build(testSpanBuilder, testMessage);
        assertTrue(invokedFirst.get());
        assertTrue(invokedSecond.get());
    }

    @Test
    void testNoOpReturnsUnadjustedSpanBuilder() {
        MessageTagBuilder.builder().build(testSpanBuilder, testMessage);
        verifyNoInteractions(testSpanBuilder);
    }
}