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
        SubscriptionQueryResult subscriptionQueryResult =
                new DefaultSubscriptionQueryResult<>(initialResult,
                                                     updates,
                                                     () -> true);
        Span span = mockTracer.buildSpan("test").start();

        // when
        TraceableSubscriptionQueryResult traceableSubscriptionQueryResult = new TraceableSubscriptionQueryResult(
                subscriptionQueryResult, span);

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
