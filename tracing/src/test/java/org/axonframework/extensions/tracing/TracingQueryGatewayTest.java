package org.axonframework.extensions.tracing;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.DefaultSubscriptionQueryResult;
import org.axonframework.queryhandling.GenericQueryResponseMessage;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryMessage;
import org.axonframework.queryhandling.QueryResponseMessage;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.axonframework.messaging.responsetypes.ResponseTypes.instanceOf;
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

    private QueryResponseMessage<String> answer1, answer2;

    @BeforeEach
    void before() {
        mockQueryBus = mock(QueryBus.class);
        mockTracer = new MockTracer();

        testSubject = TracingQueryGateway.builder()
                                         .tracer(mockTracer)
                                         .delegateQueryBus(mockQueryBus)
                                         .build();

        answer1 = new GenericQueryResponseMessage<>("answer1");
        answer2 = new GenericQueryResponseMessage<>("answer2");
    }

    @Test
    void testQuery() throws ExecutionException, InterruptedException {
        //noinspection unchecked
        when(mockQueryBus.query(any(QueryMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(answer1));

        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        try (final Scope ignored = scopeManager.activate(span)) {
            CompletableFuture<String> query = testSubject.query("query", "Query", String.class);
            assertEquals("answer1", query.get());

            // Verify the parent span is restored, and that a child span was finished.
            Span activeSpan = mockTracer.activeSpan();
            assertEquals(span, activeSpan);

            List<MockSpan> mockSpans = mockTracer.finishedSpans();
            assertEquals(1, mockSpans.size());
            assertEquals("send_query", mockSpans.get(0).operationName());
        }
        assertNull(scopeManager.activeSpan(), "There should be no activeSpan");
    }

    @Test
    void testScatterGather() {
        when(mockQueryBus.scatterGather(any(QueryMessage.class), anyLong(), any()))
                .thenReturn(Stream.of(answer1, answer2));

        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        try (final Scope ignored = scopeManager.activate(span)) {
            try (Stream<String> actual = testSubject.scatterGather("query",
                                                                   "Query",
                                                                   ResponseTypes.instanceOf(String.class),
                                                                   1L,
                                                                   TimeUnit.MILLISECONDS)) {
                Iterator<String> iterator = actual.iterator();
                String firstResult = iterator.next();
                assertEquals("answer1", firstResult);
                String secondResult = iterator.next();
                assertEquals("answer2", secondResult);
            }
            // Verify the parent span is restored, and that a child span was finished.
            Span activeSpan = mockTracer.activeSpan();
            assertEquals(span, activeSpan);

            List<MockSpan> mockSpans = mockTracer.finishedSpans();
            assertEquals(1, mockSpans.size());
            assertEquals("scatterGather_query", mockSpans.get(0).operationName());
        }
        assertNull(scopeManager.activeSpan(), "There should be no activeSpan");
    }

    @Test
    void testSubscriptionQuery() {
        when(mockQueryBus.subscriptionQuery(any(), any(), anyInt()))
                .thenReturn(new DefaultSubscriptionQueryResult<>(Mono.empty(), Flux.empty(), () -> true));

        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        try (final Scope ignored = scopeManager.activate(span)) {
            SubscriptionQueryResult<String, String> result = testSubject.subscriptionQuery(new MyQuery(),
                                                                                           instanceOf(String.class),
                                                                                           instanceOf(String.class));
            result.cancel();

            // Verify the parent span is restored, and that a child span was finished.
            Span activeSpan = mockTracer.activeSpan();
            assertEquals(span, activeSpan);

            List<MockSpan> mockSpans = mockTracer.finishedSpans();
            assertEquals(1, mockSpans.size());
            assertEquals("subscriptionQuery_MyQuery", mockSpans.get(0).operationName());
        }
        assertNull(scopeManager.activeSpan(), "There should be no activeSpan");
    }

    private static class MyQuery {

    }
}
