package org.axonframework.extensions.tracing;

import io.opentracing.propagation.TextMap;
import org.axonframework.messaging.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MapExtractor implements TextMap {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapExtractor.class);

    private Map<String, String> extracted = new HashMap<>();

    public MapExtractor(MetaData metaData) {
        metaData.entrySet().forEach(entry ->
                {
                    if (entry.getValue() instanceof String) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Extracting metadata entry: {}", entry);

                        }
                        extracted.put(entry.getKey(), (String) entry.getValue());
                    }
                }
        );
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return extracted.entrySet().iterator();
    }

    @Override
    public void put(String key, String value) {
        throw new UnsupportedOperationException("iterator should only be used with Tracer.extract()");
    }
}
