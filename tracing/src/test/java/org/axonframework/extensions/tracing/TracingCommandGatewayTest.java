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
            ((CommandCallback) invocation.getArguments()[1])
                    .onResult((CommandMessage) invocation.getArguments()[0],
                              asCommandResultMessage("result"));
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
        }
        assertNull(scopeManager.activeSpan(), "There should be no activeSpan");
    }

    private static class MyCommand {

    }
}
