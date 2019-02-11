package org.axonframework.extensions.tracing;

import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.axonframework.commandhandling.GenericCommandResultMessage.asCommandResultMessage;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

public class TracingCommandGatewayTest {


    private CommandBus mockCommandBus;
    private MockTracer mockTracer;

    private TracingCommandGateway testSubject;

    @Before
    public void before() {
        mockCommandBus = mock(CommandBus.class);
        mockTracer = new MockTracer();

        DefaultCommandGateway.Builder builder = DefaultCommandGateway.builder().commandBus(mockCommandBus);
        testSubject = new TracingCommandGateway(builder, mockTracer);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSendWithCallback() {

        doAnswer(invocation -> {
            ((CommandCallback) invocation.getArguments()[1])
                    .onResult((CommandMessage) invocation.getArguments()[0],
                            asCommandResultMessage("result"));
            return null;
        }).when(mockCommandBus).dispatch(isA(CommandMessage.class), isA(CommandCallback.class));


        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        scopeManager.activate(span, false);


        testSubject.send("Command", (m, r) -> {
            // Call back.
            assertThat(r, notNullValue());
        });

        verify(mockCommandBus, times(1)).dispatch(isA(CommandMessage.class), isA(CommandCallback.class));

        // Verify the parent span is restored, and that a child span was finished.
        Span activeSpan = mockTracer.activeSpan();
        assertThat(activeSpan, is(span));

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertThat(mockSpans.size(), is(1));
        assertThat(mockSpans.get(0).operationName(), is("command"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSendWithoutCallback() throws ExecutionException, InterruptedException {

        doAnswer(invocation -> {
            ((CommandCallback) invocation.getArguments()[1])
                    .onResult((CommandMessage) invocation.getArguments()[0],
                            asCommandResultMessage("result"));
            return null;
        }).when(mockCommandBus).dispatch(isA(CommandMessage.class), isA(CommandCallback.class));

        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        scopeManager.activate(span, false);


        CompletableFuture<Object> future = testSubject.send("Command");

        verify(mockCommandBus, times(1)).dispatch(isA(CommandMessage.class), isA(CommandCallback.class));

        assertThat(future.isDone(), is(true));
        assertThat(future.get(), is("result"));

        // Verify the parent span is restored, and that a child span was finished.
        Span activeSpan = mockTracer.activeSpan();
        assertThat(activeSpan, is(span));

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertThat(mockSpans.size(), is(1));
        assertThat(mockSpans.get(0).operationName(), is("command"));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSendAndWait() {

        doAnswer(invocation -> {
            ((CommandCallback) invocation.getArguments()[1])
                    .onResult((CommandMessage) invocation.getArguments()[0],
                            asCommandResultMessage("result"));
            return null;
        }).when(mockCommandBus).dispatch(isA(CommandMessage.class), isA(CommandCallback.class));

        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        scopeManager.activate(span, false);

        Object result = testSubject.sendAndWait("Command");

        verify(mockCommandBus, times(1)).dispatch(isA(CommandMessage.class), isA(CommandCallback.class));

        assertThat(result, instanceOf(String.class));
        assertThat(result, is("result"));


        Span activeSpan = mockTracer.activeSpan();
        assertThat(activeSpan, is(span));

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertThat(mockSpans.size(), is(1));
        assertThat(mockSpans.get(0).operationName(), is("command"));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSendAndWaitWithTimeout() {

        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        scopeManager.activate(span, false);

        Object result = testSubject.sendAndWait("Command", 10, TimeUnit.MILLISECONDS);

        verify(mockCommandBus, times(1)).dispatch(isA(CommandMessage.class), isA(CommandCallback.class));

        assertThat(result, nullValue());


        Span activeSpan = mockTracer.activeSpan();
        assertThat(activeSpan, is(span));

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertThat(mockSpans.size(), is(0));
    }

}