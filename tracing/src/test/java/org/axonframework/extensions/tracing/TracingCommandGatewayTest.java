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

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.axonframework.commandhandling.GenericCommandResultMessage.asCommandResultMessage;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for the {@link TracingCommandGateway}.
 *
 * @author Christophe Bouhier
 * @author Steven van Beelen
 */
class TracingCommandGatewayTest {

    private CommandBus mockCommandBus;
    private MockTracer mockTracer;

    private TracingCommandGateway testSubject;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void before() {
        mockTracer = new MockTracer();
        mockCommandBus = mock(CommandBus.class);

        doAnswer(invocation -> {
            //noinspection rawtypes
            ((CommandCallback<?, ?>) invocation.getArguments()[1])
                    .onResult((CommandMessage) invocation.getArguments()[0], asCommandResultMessage("result"));
            return null;
        }).when(mockCommandBus).dispatch(isA(CommandMessage.class), isA(CommandCallback.class));

        testSubject = TracingCommandGateway.builder()
                                           .tracer(mockTracer)
                                           .delegateCommandBus(mockCommandBus)
                                           .build();
    }

    @Test
    void testSendWithCallback() {
        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        try (final Scope ignored = scopeManager.activate(span)) {

            testSubject.send(new MyCommand(), (m, r) -> {
                // Call back.
                assertNotNull(r);
            });

            //noinspection unchecked
            verify(mockCommandBus, times(1))
                    .dispatch(isA(CommandMessage.class), isA(CommandCallback.class));

            // Verify the parent span is restored, and that a child span was finished.
            Span activeSpan = mockTracer.activeSpan();
            assertEquals(span, activeSpan);

            List<MockSpan> mockSpans = mockTracer.finishedSpans();
            assertEquals(1, mockSpans.size());
            assertEquals("send_MyCommand", mockSpans.get(0).operationName());
            assertNotNull(mockSpans.get(0).logEntries());
            assertFalse(mockSpans.get(0).logEntries().isEmpty());
            assertNotNull(mockSpans.get(0).tags());
            assertFalse(mockSpans.get(0).tags().isEmpty());
        }
        assertNull(scopeManager.activeSpan(), "There should be no activeSpan");
    }


    @Test
    void testSendWithoutCallback() throws ExecutionException, InterruptedException {

        MockSpan span = mockTracer.buildSpan("test").start();

        ScopeManager scopeManager = mockTracer.scopeManager();
        try (final Scope ignored = scopeManager.activate(span)) {
            CompletableFuture<Object> future = testSubject.send(new MyCommand());

            //noinspection unchecked
            verify(mockCommandBus, times(1))
                    .dispatch(isA(CommandMessage.class), isA(CommandCallback.class));

            assertTrue(future.isDone());
            assertEquals("result", future.get());

            // Verify the parent span is restored, and that a child span was finished.
            Span activeSpan = mockTracer.activeSpan();
            assertEquals(span, activeSpan);

            List<MockSpan> mockSpans = mockTracer.finishedSpans();
            assertEquals(1, mockSpans.size());
            assertEquals("send_MyCommand", mockSpans.get(0).operationName());
            assertNotNull(mockSpans.get(0).logEntries());
            assertFalse(mockSpans.get(0).logEntries().isEmpty());
            assertNotNull(mockSpans.get(0).tags());
            assertFalse(mockSpans.get(0).tags().isEmpty());
        }
        assertNull(scopeManager.activeSpan(), "There should be no activeSpan");
    }

    @Test
    void testSendAndWait() {
        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        try (final Scope ignored = scopeManager.activate(span)) {

            Object result = testSubject.sendAndWait(new MyCommand());

            //noinspection unchecked
            verify(mockCommandBus, times(1))
                    .dispatch(isA(CommandMessage.class), isA(CommandCallback.class));

            assertTrue(result instanceof String);
            assertEquals("result", result);

            Span activeSpan = mockTracer.activeSpan();
            assertEquals(span, activeSpan);

            List<MockSpan> mockSpans = mockTracer.finishedSpans();
            assertEquals(1, mockSpans.size());
            assertEquals("sendAndWait_MyCommand", mockSpans.get(0).operationName());
            assertNotNull(mockSpans.get(0).logEntries());
            assertFalse(mockSpans.get(0).logEntries().isEmpty());
            assertNotNull(mockSpans.get(0).tags());
            assertFalse(mockSpans.get(0).tags().isEmpty());
        }
        assertNull(scopeManager.activeSpan(), "There should be no activeSpan");
    }

    @Test
    void testSendAndWaitWithTimeout() {
        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        try (final Scope ignored = scopeManager.activate(span)) {

            Object result = testSubject.sendAndWait(new MyCommand(), 10, TimeUnit.MILLISECONDS);

            //noinspection unchecked
            verify(mockCommandBus, times(1))
                    .dispatch(isA(CommandMessage.class), isA(CommandCallback.class));

            assertTrue(result instanceof String);
            assertEquals("result", result);

            Span activeSpan = mockTracer.activeSpan();
            assertEquals(span, activeSpan);

            List<MockSpan> mockSpans = mockTracer.finishedSpans();
            assertEquals(1, mockSpans.size());
            assertEquals("sendAndWait_MyCommand", mockSpans.get(0).operationName());
            assertNotNull(mockSpans.get(0).logEntries());
            assertFalse(mockSpans.get(0).logEntries().isEmpty());
            assertNotNull(mockSpans.get(0).tags());
            assertFalse(mockSpans.get(0).tags().isEmpty());
        }
        assertNull(scopeManager.activeSpan(), "There should be no activeSpan");
    }

    private static class MyCommand {

    }
}
