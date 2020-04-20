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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validate behaviour of the {@link TracingProvider}.
 *
 * @author Christophe Bouhier
 */
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
            Message<String> message = new GenericMessage<>("payload", MetaData.emptyInstance());

            TracingProvider tracingProvider = new TracingProvider(mockTracer);

            Map<String, ?> correlated = tracingProvider.correlationDataFor(message);

            assertEquals(String.valueOf(span.context().spanId()), correlated.get("spanid"));
            assertEquals(String.valueOf(span.context().traceId()), correlated.get("traceid"));
        }
    }

    @Test
    void testTracingProviderEmptyTraceContext() {
        Message<String> message = new GenericMessage<>("payload", MetaData.emptyInstance());
        TracingProvider tracingProvider = new TracingProvider(mockTracer);
        Map<String, ?> correlated = tracingProvider.correlationDataFor(message);
        assertTrue(correlated.isEmpty());
    }
}
