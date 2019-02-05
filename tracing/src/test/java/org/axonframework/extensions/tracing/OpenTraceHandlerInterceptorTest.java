package org.axonframework.extensions.tracing;

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
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
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
        scopeManager.activate(test, true);

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

        assertThat(mockSpan.operationName(), is("Extracting"));
        assertThat(mockSpan.tags().get(OpenTraceHandlerInterceptor.TAG_AXON_ID), is(message.getIdentifier()));
        assertThat(mockSpan.tags().get(OpenTraceHandlerInterceptor.TAG_AXON_PAYLOAD_TYPE), is("java.lang.String"));

        assertThat(mockSpan.tags().get(Tags.SPAN_KIND.getKey()), is(Tags.SPAN_KIND_SERVER));
    }

    @After
    public void after(){
        mockTracer.scopeManager().active().close();
    }
}