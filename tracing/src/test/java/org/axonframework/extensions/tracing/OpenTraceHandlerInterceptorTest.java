package org.axonframework.extensions.tracing;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import org.axonframework.messaging.GenericMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.unitofwork.DefaultUnitOfWork;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class OpenTraceHandlerInterceptorTest {

    private MockTracer mockTracer;
    private OpenTraceHandlerInterceptor openTraceDispatchInterceptor;
    private DefaultUnitOfWork<Message<?>> unitOfWork;
    private InterceptorChain mockInterceptorChain;

    @Before
    public void before() {
        mockTracer = new MockTracer();
        openTraceDispatchInterceptor = new OpenTraceHandlerInterceptor(mockTracer);
        mockInterceptorChain = mock(InterceptorChain.class);
        unitOfWork = new DefaultUnitOfWork<>(null);
    }

    @Test
    public void testHandle() throws Exception {
        MockSpan test = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        try(final Scope ignored = scopeManager.activate(test)) {
            Message message = new GenericMessage<Object>("Payload").withMetaData(new HashMap<String, String>() {{
                put("spanid", "1");
                put("traceid", "2");
            }});

            unitOfWork.transformMessage(m -> message);

            openTraceDispatchInterceptor.handle(unitOfWork, mockInterceptorChain);

            // Push the state, so the child span is finished.
            unitOfWork.start();
            unitOfWork.commit();

            List<MockSpan> mockSpans = mockTracer.finishedSpans();
            assertThat(mockSpans.size(), is(1));
            MockSpan mockSpan = mockSpans.get(0);
            assertThat(mockSpan.parentId(), is(1L));
            assertThat(mockSpan.context().traceId(), is(2L));

            assertThat(mockSpan.operationName(), is("handleMessage"));
            assertThat(mockSpan.tags().get("axon.message.id"), is(message.getIdentifier()));
            assertThat(mockSpan.tags().get("axon.message.type"), is("Message"));
            assertThat(mockSpan.tags().get("axon.message.payloadtype"), is("java.lang.String"));

            assertThat(mockSpan.tags().get(Tags.SPAN_KIND.getKey()), is(Tags.SPAN_KIND_SERVER));
        }
    }
}
