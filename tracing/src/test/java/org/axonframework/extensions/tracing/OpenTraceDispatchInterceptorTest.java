/*
 * Copyright (c) 2010-2020. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 * Validate behaviour of the {@link OpenTraceDispatchInterceptor}.
 *
 * @author Christophe Bouhier
 */
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
