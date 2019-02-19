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

import io.opentracing.propagation.TextMap;
import org.axonframework.messaging.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An implementation of {@link TextMap}, to inject tracing fields with {@link MetaData}.
 *
 * @author Christophe Bouhier
 * @since 4.0
 */
public class MapInjector implements TextMap {

    private static final Logger logger = LoggerFactory.getLogger(MapInjector.class);

    private Map<String, String> metaData = new HashMap<>();

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("The iterator() method should never be used with Tracer.inject()");
    }

    @Override
    public void put(String key, String value) {
        if (logger.isDebugEnabled()) {
            logger.debug("Injecting tracing property key [{}] with value [{}]", key, value);
        }
        metaData.put(key, value);
    }

    /**
     * Retrieve a {@link Map} of {@link String} to String of all the injected tracing fields used as input for
     * {@link MetaData}.
     *
     * @return a {@link Map} of {@link String} to String  of all the injected tracing fields used as input for
     * {@link MetaData}
     */
    public Map<String, String> getMetaData() {
        return metaData;
    }
}
