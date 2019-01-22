package org.axonframework.extensions.tracing;

import io.opentracing.*;
import io.opentracing.propagation.Format;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MessageDispatchInterceptor;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class OpenTraceDispatchInterceptor implements MessageDispatchInterceptor<Message<?>> {

    private final Tracer tracer;

    public OpenTraceDispatchInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public BiFunction<Integer, Message<?>, Message<?>> handle(List<? extends Message<?>> messages) {

        ScopeManager scopeManager = tracer.scopeManager();
        Optional<SpanContext> spanContextOptional = Optional.ofNullable(scopeManager)
                .map(ScopeManager::active)
                .map(Scope::span)
                .map(Span::context);

        if (spanContextOptional.isPresent()) {
            Optional<? extends Message<?>> singleMessage = messages.stream()
                    .map(m -> {
                        MapInjector injector = new MapInjector();
                        tracer.inject(spanContextOptional.get(), Format.Builtin.TEXT_MAP, injector);
                        return m.andMetaData(injector.getMetaData());
                    }).findAny();
            return singleMessage.<BiFunction<Integer, Message<?>, Message<?>>>map(message -> (i, m) -> message).orElseGet(() -> (i, m) -> m);
        } else {
            return (i, m) -> m;
        }
    }
}
