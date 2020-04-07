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

import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MetaData;
import org.axonframework.messaging.correlation.CorrelationDataProvider;

import java.util.Collections;
import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * A {@link CorrelationDataProvider} which uses a {@link MapInjector} to inject a message {@link MetaData} with the
 * active span.
 *
 * @author Christophe Bouhier
 * @since 4.0
 */
public class TracingProvider implements CorrelationDataProvider {

    private Tracer tracer;

    /**
     * Initialize a {@link CorrelationDataProvider} implementation which uses the provided {@link Tracer} to set the
     * active span on a {@link Message}'s {@link MetaData}.
     *
     * @param tracer the {@link Tracer} used to retrieve the active span to be placed on a {@link Message}'s {@link
     *               MetaData}
     */
    public TracingProvider(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Map<String, ?> correlationDataFor(Message<?> message) {
        return ofNullable(tracer.activeSpan())
                .map(activeSpan -> {
                    MapInjector injector = new MapInjector();
                    tracer.inject(activeSpan.context(), Format.Builtin.TEXT_MAP, injector);
                    return injector.getMetaData();
                })
                .orElseGet(Collections::emptyMap);
    }
}