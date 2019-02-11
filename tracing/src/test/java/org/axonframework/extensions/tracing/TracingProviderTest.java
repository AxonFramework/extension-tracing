package org.axonframework.extensions.tracing;

import io.opentracing.ScopeManager;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.axonframework.messaging.GenericMessage;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MetaData;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TracingProviderTest {

    private MockTracer mockTracer;

    @Before
    public void before() {
        mockTracer = new MockTracer();
        ScopeManager scopeManager = mockTracer.scopeManager();
    }

    @Test
    public void testTracingProvider() {

        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        scopeManager.activate(span, true);

        Message message = new GenericMessage<>("payload", MetaData.emptyInstance());

        TracingProvider tracingProvider = new TracingProvider(mockTracer);

        Map<String, ?> correlated = tracingProvider.correlationDataFor(message);

        assertThat(correlated.get("spanid"), is(Long.valueOf(span.context().spanId()).toString()));
        assertThat(correlated.get("traceid"), is(Long.valueOf(span.context().traceId()).toString()));
    }

    @Test
    public void testTracingProviderEmptyTraceContext() {

        Message message = new GenericMessage<>("payload", MetaData.emptyInstance());
        TracingProvider tracingProvider = new TracingProvider(mockTracer);
        Map<String, ?> correlated = tracingProvider.correlationDataFor(message);
        assertThat(correlated.isEmpty(), is(true));
    }

}