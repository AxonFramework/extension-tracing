package org.axonframework.extensions.tracing;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.axonframework.queryhandling.GenericQueryResponseMessage;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryMessage;
import org.axonframework.queryhandling.QueryResponseMessage;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for the {@link TracingQueryGateway}.
 *
 * @author Christophe Bouhier
 * @author Steven van Beelen
 */
class TracingQueryGatewayTest {

    private QueryBus mockQueryBus;
    private MockTracer mockTracer;

    private TracingQueryGateway testSubject;

    private QueryResponseMessage<String> answer;

    @BeforeEach
    void before() {
        mockQueryBus = mock(QueryBus.class);
        mockTracer = new MockTracer();

        testSubject = TracingQueryGateway.builder()
                                         .tracer(mockTracer)
                                         .delegateQueryBus(mockQueryBus)
                                         .build();

        answer = new GenericQueryResponseMessage<>("answer");
    }

    @Test
    void testQuery() throws ExecutionException, InterruptedException {
        //noinspection unchecked
        when(mockQueryBus.query(any(QueryMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(answer));

        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        try (final Scope ignored = scopeManager.activate(span)) {
            CompletableFuture<String> query = testSubject.query("query", "Query", String.class);
            assertEquals("answer", query.get());

            // Verify the parent span is restored, and that a child span was finished.
            Span activeSpan = mockTracer.activeSpan();
            assertEquals(span, activeSpan);

            List<MockSpan> mockSpans = mockTracer.finishedSpans();
            assertEquals(1, mockSpans.size());
            assertEquals("send_query", mockSpans.get(0).operationName());
        }
    }
}
