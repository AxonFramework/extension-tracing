package org.axonframework.extensions.tracing;

import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.axonframework.queryhandling.GenericQueryResponseMessage;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryMessage;
import org.axonframework.queryhandling.QueryResponseMessage;
import org.hamcrest.CoreMatchers;
import org.junit.*;
import org.mockito.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TracingQueryGatewayTest {

    private QueryBus mockQueryBus;
    private MockTracer mockTracer;

    private TracingQueryGateway testSubject;

    private QueryResponseMessage<String> answer;

    @Before
    public void before() {
        mockQueryBus = mock(QueryBus.class);
        mockTracer = new MockTracer();

        testSubject = TracingQueryGateway.builder()
                                         .queryBus(mockQueryBus)
                                         .tracer(mockTracer)
                                         .build();
        answer = new GenericQueryResponseMessage<>("answer");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testQuery() throws ExecutionException, InterruptedException {
        when(mockQueryBus.query(ArgumentMatchers.any(QueryMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(answer));


        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        scopeManager.activate(span, false);

        CompletableFuture<String> query = testSubject.query("query", "Query", String.class);
        assertThat(query.get(), CoreMatchers.is("answer"));


        // Verify the parent span is restored, and that a child span was finished.
        Span activeSpan = mockTracer.activeSpan();
        assertThat(activeSpan, is(span));

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertThat(mockSpans.size(), is(1));
        assertThat(mockSpans.get(0).operationName(), is("query"));
    }
}
