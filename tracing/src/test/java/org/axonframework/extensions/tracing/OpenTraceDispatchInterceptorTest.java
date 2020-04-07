package org.axonframework.extensions.tracing;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.axonframework.messaging.GenericMessage;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MetaData;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class OpenTraceDispatchInterceptorTest {

    private OpenTraceDispatchInterceptor openTraceDispatchInterceptor;
    private MockTracer mockTracer;

    @BeforeEach
    void before() {
        mockTracer = new MockTracer();
        openTraceDispatchInterceptor = new OpenTraceDispatchInterceptor(mockTracer);
    }

    @Test
    void testDispatch() {
        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        try (final Scope ignored = scopeManager.activate(span)) {
            GenericMessage<String> msg = new GenericMessage<>("Payload");
            BiFunction<Integer, Message<?>, Message<?>> handle =
                    openTraceDispatchInterceptor.handle(Collections.singletonList(msg));

            // This interceptor has no side effects on the spans in the tracer.
            List<MockSpan> mockSpans = mockTracer.finishedSpans();
            assertTrue(mockSpans.isEmpty());

            Message<?> apply = handle.apply(0, msg);
            MetaData metaData = apply.getMetaData();
            assertEquals(2, metaData.size());
            assertEquals(String.valueOf(span.context().spanId()), metaData.get("spanid"));
            assertEquals(String.valueOf(span.context().traceId()), metaData.get("traceid"));
        }
    }
}
