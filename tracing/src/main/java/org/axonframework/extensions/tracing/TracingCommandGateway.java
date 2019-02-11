/*
 * Copyright (c) 2010-2019. Axon Framework
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
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.CommandResultMessage;
import org.axonframework.commandhandling.callbacks.FutureCallback;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A tracing command gateway which activates a calling {@link Span}, when the {@link CompletableFuture} completes.
 *
 * @author Christophe Bouhier
 * @since 4.0
 */
public class TracingCommandGateway extends DefaultCommandGateway {

    private final Tracer tracer;

    public TracingCommandGateway(Builder builder, Tracer tracer) {
        super(builder);
        this.tracer = tracer;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> CompletableFuture<R> send(Object command) {
        return (CompletableFuture<R>) super.send(command);
    }

    @Override
    public <C, R> void send(C command, CommandCallback<? super C, ? super R> callback) {

        sendWithSpan(tracer, (tracer, parentSpan, childSpan) -> {
            super.send(command, callback);
            childSpan.finish();
            tracer.scopeManager().activate(parentSpan, false);
        });
    }

    @Override
    public <R> R sendAndWait(Object command) {

        FutureCallback<Object, R> futureCallback = new FutureCallback<>();

        sendAndRestoreParentSpan(command, futureCallback);
        CommandResultMessage<? extends R> commandResultMessage = futureCallback.getResult();
        if (commandResultMessage.isExceptional()) {
            throw asRuntime(commandResultMessage.exceptionResult());
        }
        return commandResultMessage.getPayload();
    }

    @Override
    public <R> R sendAndWait(Object command, long timeout, TimeUnit unit) {

        FutureCallback<Object, R> futureCallback = new FutureCallback<>();

        sendAndRestoreParentSpan(command, futureCallback);
        CommandResultMessage<? extends R> commandResultMessage = futureCallback.getResult(timeout, unit);
        if (commandResultMessage.isExceptional()) {
            throw asRuntime(commandResultMessage.exceptionResult());
        }
        return commandResultMessage.getPayload();
    }

    private <R> void sendAndRestoreParentSpan(Object command, FutureCallback<Object, R> futureCallback) {
        sendWithSpan(tracer, (tracer, parentSpan, childSpan) -> {
            super.send(command, futureCallback);
            futureCallback.whenComplete((r, e) -> {
                childSpan.finish();
                tracer.scopeManager().activate(parentSpan, false);
            });
        });
    }

    private RuntimeException asRuntime(Throwable e) {
        Throwable failure = e.getCause();
        if (failure instanceof Error) {
            throw (Error) failure;
        } else if (failure instanceof RuntimeException) {
            return (RuntimeException) failure;
        } else {
            return new CommandExecutionException("An exception occurred while executing a command", failure);
        }
    }

    private void sendWithSpan(Tracer tracer, SpanConsumer consumer) {
        Span parent = tracer.activeSpan();
        try (Scope scope = tracer.buildSpan("command").startActive(false)) {
            Span span = scope.span();
            consumer.accept(tracer, parent, span);
        }
    }

    private <R> R sendWithSpan(Tracer tracer, SpanFunction<R> function) {
        Span parent = tracer.activeSpan();
        try (Scope scope = tracer.buildSpan("command").startActive(false)) {
            Span span = scope.span();
            return function.accept(tracer, parent, span);
        }
    }

    @FunctionalInterface
    public interface SpanConsumer {
        void accept(Tracer tracer, Span activeSpan, Span parentSpan);
    }

    @FunctionalInterface
    public interface SpanFunction<R> {
        R accept(Tracer tracer, Span activeSpan, Span parentSpan);
    }
}
