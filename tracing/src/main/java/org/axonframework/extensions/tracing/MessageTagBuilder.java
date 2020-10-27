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

import io.opentracing.Tracer;
import org.axonframework.messaging.Message;

import java.util.function.BiFunction;

/**
  * Functional interface towards adding {@link Message} specific information as tags to a {@link
 * io.opentracing.Tracer.SpanBuilder}.
 *
 * @author Corrado Musumeci
 * @author Steven van Beelen
 * @since 4.4
 */
@FunctionalInterface
public interface MessageTagBuilder extends BiFunction<Tracer.SpanBuilder, Message<?>, Tracer.SpanBuilder> {

    /**
     * Build the new {@link Tracer.SpanBuilder} based on the given {@code spanBuilder} and {@code message}.
     *
     * @param spanBuilder the {@link Tracer.SpanBuilder} to add the given {@code message}'s information to as tags
     * @param message     the {@link Message} to retrieve information from as the tags for the given {@code
     *                    spanBuilder}
     * @return a {@link Tracer.SpanBuilder} with additional tag information attached to it based on the given {@code
     * message}
     */
    default Tracer.SpanBuilder build(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        return apply(spanBuilder, message);
    }

    /**
     * Combine {@code this} {@link MessageTagBuilder} with the given {@code messageTagBuilder}.
     *
     * @param messageTagBuilder the {@link MessageTagBuilder} to combine with {@code this} {@code MessageTagBuilder}
     * @return a {@link MessageTagBuilder} consisting out of {@code this} {@link MessageTagBuilder} and the given {@code
     * messageTagBuilder}
     */
    default MessageTagBuilder with(MessageTagBuilder messageTagBuilder) {
        return (spanBuilder, message) -> {
            Tracer.SpanBuilder first = this.build(spanBuilder, message);
            return messageTagBuilder.build(first, message);
        };
    }

    /**
     * Instantiate an empty {@link MessageTagBuilder}, to for example append additional {@code MessageTagBuilder}
     * instance too through {@link #with(MessageTagBuilder)}
     *
     * @return an empty {@link MessageTagBuilder}
     */
    static MessageTagBuilder builder() {
        return (spanBuilder, message) -> spanBuilder;
    }
}
