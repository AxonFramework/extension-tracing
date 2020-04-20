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

import io.opentracing.Span;
import io.opentracing.mock.MockTracer;
import org.axonframework.queryhandling.DefaultSubscriptionQueryResult;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Test class for the {@link DefaultSubscriptionQueryResult}.
 *
 * @author Lucas Campos
 */
class TraceableSubscriptionQueryResultTest {

    private MockTracer mockTracer;

    @BeforeEach
    void before() {
        this.mockTracer = new MockTracer();
    }

    @Test
    void testTraceableSubscriptionQueryResult() {
        // given
        Mono<String> initialResult = Mono.just("initial");
        Flux<String> updates = Flux.just("update1", "update2");
        SubscriptionQueryResult<String, String> subscriptionQueryResult =
                new DefaultSubscriptionQueryResult<>(initialResult,
                                                     updates,
                                                     () -> true);
        Span span = mockTracer.buildSpan("test").start();

        // when
        TraceableSubscriptionQueryResult<String, String> traceableSubscriptionQueryResult =
                new TraceableSubscriptionQueryResult<>(subscriptionQueryResult, span);

        // verify
        StepVerifier.create(traceableSubscriptionQueryResult.initialResult())
                    .expectNext("initial")
                    .expectComplete()
                    .verify();
        StepVerifier.create(traceableSubscriptionQueryResult.updates())
                    .expectNext("update1")
                    .expectNext("update2")
                    .expectComplete()
                    .verify();
    }
}
