package org.axonframework.extensions.tracing;

import io.opentracing.ScopeManager;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.unitofwork.DefaultUnitOfWork;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenTraceHandlerInterceptorTest {

    private MockTracer mockTracer;
    private OpenTraceHandlerInterceptor openTraceDispatchInterceptor;
    private DefaultUnitOfWork<Message<?>> unitOfWork;
    private InterceptorChain mockInterceptorChain;

    @BeforeEach
    void before() {
        mockTracer = new MockTracer();
        openTraceDispatchInterceptor = new OpenTraceHandlerInterceptor(mockTracer);
        mockInterceptorChain = mock(InterceptorChain.class);
        unitOfWork = new DefaultUnitOfWork<>(null);
    }

    @Test
    void testHandle() throws Exception {
        MockSpan test = mockTracer.buildSpan("test").start();
        ScopeManager scopeManager = mockTracer.scopeManager();
        scopeManager.activate(test);

        Message message = new GenericDomainEventMessage<MyEvent>("Payload", "aggregate_1", 0, new MyEvent())
                .withMetaData(new HashMap<String, String>() {{
                    put("spanid", "1");
                    put("traceid", "2");
                }});

        unitOfWork.transformMessage(m -> message);

        openTraceDispatchInterceptor.handle(unitOfWork, mockInterceptorChain);

        // Push the state, so the child span is finished.
        unitOfWork.start();
        unitOfWork.commit();

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertEquals(1, mockSpans.size());
        MockSpan mockSpan = mockSpans.get(0);
        assertEquals(1, mockSpan.parentId());
        assertEquals(2, mockSpan.context().traceId());

        assertEquals("handle_MyEvent", mockSpan.operationName());
        assertEquals(message.getIdentifier(), mockSpan.tags().get("axon.message.id"));
        assertEquals("MyEvent", mockSpan.tags().get("axon.message.type"));
        assertEquals("aggregate_1", mockSpan.tags().get("axon.message.aggregateIdentifier"));
        assertEquals("org.axonframework.extensions.tracing.OpenTraceHandlerInterceptorTest$MyEvent",
                     mockSpan.tags().get("axon.message.payloadtype"));

        assertEquals(Tags.SPAN_KIND_SERVER, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
    }

    private static class MyEvent {

    }
}
