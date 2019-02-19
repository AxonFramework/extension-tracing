package org.axonframework.extensions.tracing;

import io.opentracing.ScopeManager;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.axonframework.messaging.GenericMessage;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MetaData;
import org.junit.*;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import static java.lang.Long.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class OpenTraceDispatchInterceptorTest {

    private OpenTraceDispatchInterceptor openTraceDispatchInterceptor;
    private MockTracer mockTracer;

    @Before
    public void before() {
        mockTracer = new MockTracer();
        openTraceDispatchInterceptor = new OpenTraceDispatchInterceptor(mockTracer);
    }

    @After
    public void after() {
        mockTracer.scopeManager().active().close();
    }

    @Test
    public void testDispatch() {
        MockSpan span = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        scopeManager.activate(span, false);

        GenericMessage<String> msg = new GenericMessage<>("Payload");
        BiFunction<Integer, Message<?>, Message<?>> handle =
                openTraceDispatchInterceptor.handle(Collections.singletonList(msg));

        // This interceptor has no side effects on the spans in the tracer.
        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertThat(mockSpans.isEmpty(), is(true));

        Message<?> apply = handle.apply(0, msg);
        MetaData metaData = apply.getMetaData();
        assertThat(metaData.size(), is(2));
        assertThat(metaData.get("spanid"), is(valueOf(span.context().spanId()).toString()));
        assertThat(metaData.get("traceid"), is(valueOf(span.context().traceId()).toString()));
    }
}