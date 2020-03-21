package org.axonframework.extensions.tracing;

import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.junit.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.axonframework.commandhandling.GenericCommandResultMessage.asCommandResultMessage;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

/**
 * Test class for the {@link TracingCommandGateway}.
 *
 * @author Christophe Bouhier
 * @author Steven van Beelen
 */
public class TracingCommandGatewayTest {

    private CommandBus mockCommandBus;
    private MockTracer mockTracer;

    private TracingCommandGateway testSubject;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
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
    public void testSendWithCallback() {
        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        scopeManager.activate(span, false);

        testSubject.send(new MyCommand(), (m, r) -> {
            // Call back.
            assertThat(r, notNullValue());
        });

        //noinspection unchecked
        verify(mockCommandBus, times(1)).dispatch(isA(CommandMessage.class), isA(CommandCallback.class));

        // Verify the parent span is restored, and that a child span was finished.
        Span activeSpan = mockTracer.activeSpan();
        assertThat(activeSpan, is(span));

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertThat(mockSpans.size(), is(1));
        assertThat(mockSpans.get(0).operationName(), is("send_MyCommand"));

        mockTracer.scopeManager().active().close();
        assertThat(mockTracer.scopeManager().active(), nullValue());
    }

    private static class MyCommand {
    }

    @Test
    public void testSendWithoutCallback() throws ExecutionException, InterruptedException {
        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        scopeManager.activate(span, false);

        CompletableFuture<Object> future = testSubject.send(new MyCommand());

        //noinspection unchecked
        verify(mockCommandBus, times(1)).dispatch(isA(CommandMessage.class), isA(CommandCallback.class));

        assertThat(future.isDone(), is(true));
        assertThat(future.get(), is("result"));

        // Verify the parent span is restored, and that a child span was finished.
        Span activeSpan = mockTracer.activeSpan();
        assertThat(activeSpan, is(span));

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertThat(mockSpans.size(), is(1));
        assertThat(mockSpans.get(0).operationName(), is("send_MyCommand"));

        mockTracer.scopeManager().active().close();
        assertThat(mockTracer.scopeManager().active(), nullValue());
    }

    @Test
    public void testSendAndWait() {
        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        scopeManager.activate(span, false);

        Object result = testSubject.sendAndWait(new MyCommand());

        //noinspection unchecked
        verify(mockCommandBus, times(1)).dispatch(isA(CommandMessage.class), isA(CommandCallback.class));

        assertThat(result, instanceOf(String.class));
        assertThat(result, is("result"));

        Span activeSpan = mockTracer.activeSpan();
        assertThat(activeSpan, is(span));

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertThat(mockSpans.size(), is(1));
        assertThat(mockSpans.get(0).operationName(), is("sendAndWait_MyCommand"));

        mockTracer.scopeManager().active().close();
        assertThat(mockTracer.scopeManager().active(), nullValue());
    }

    @Test
    public void testSendAndWaitWithTimeout() {
        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        scopeManager.activate(span, false);

        Object result = testSubject.sendAndWait(new MyCommand(), 10, TimeUnit.MILLISECONDS);

        //noinspection unchecked
        verify(mockCommandBus, times(1)).dispatch(isA(CommandMessage.class), isA(CommandCallback.class));

        assertThat(result, instanceOf(String.class));
        assertThat(result, is("result"));

        Span activeSpan = mockTracer.activeSpan();
        assertThat(activeSpan, is(span));

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertThat(mockSpans.size(), is(1));
        assertThat(mockSpans.get(0).operationName(), is("sendAnd_WaitMyCommand"));

        mockTracer.scopeManager().active().close();
        assertThat(mockTracer.scopeManager().active(), nullValue());
    }
}
