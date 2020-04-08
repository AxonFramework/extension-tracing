package org.axonframework.extensions.tracing;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.axonframework.messaging.GenericMessage;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MetaData;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TracingProviderTest {

    private MockTracer mockTracer;

    @BeforeEach
    public void before() {
        mockTracer = new MockTracer();
    }

    @Test
    void testTracingProvider() {
        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        try (final Scope ignored = scopeManager.activate(span)) {

            Message message = new GenericMessage<>("payload", MetaData.emptyInstance());

            TracingProvider tracingProvider = new TracingProvider(mockTracer);

            Map<String, ?> correlated = tracingProvider.correlationDataFor(message);

            assertEquals(String.valueOf(span.context().spanId()), correlated.get("spanid"));
            assertEquals(String.valueOf(span.context().traceId()), correlated.get("traceid"));
        }
    }

    @Test
    void testTracingProviderEmptyTraceContext() {
        Message message = new GenericMessage<>("payload", MetaData.emptyInstance());
        TracingProvider tracingProvider = new TracingProvider(mockTracer);
        Map<String, ?> correlated = tracingProvider.correlationDataFor(message);
        assertTrue(correlated.isEmpty());
    }
}
