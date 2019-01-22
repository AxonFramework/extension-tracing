package org.axonframework.extensions.tracing;

import io.opentracing.propagation.TextMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class MapInjector implements TextMap {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapInjector.class);

    private Map<String, String> metaData = new HashMap<>();

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("iterator should never be used with Tracer.inject()");
    }

    @Override
    public void put(String key, String value) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Injecting tracing property key: {} , value: {}", key, value);
        }
        metaData.put(key, value);
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }
}
