package org.axonframework.extensions.tracing;

import org.axonframework.messaging.MetaData;
import org.junit.jupiter.api.*;

import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapExtractorTest {

    @Test
    void testExtractor() {
        MetaData metaData = MetaData.emptyInstance();
        metaData = metaData.and("key", "value");

        MapExtractor extractor = new MapExtractor(metaData);
        Iterator<Map.Entry<String, String>> iterator = extractor.iterator();

        assertTrue(iterator.hasNext());
        assertEquals("value", iterator.next().getValue());
    }

    @Test
    void testMapExtractorPut_throwsUnsupportedOperationException() {
        MetaData metaData = MetaData.emptyInstance();
        metaData = metaData.and("key", "value");

        MapExtractor extractor = new MapExtractor(metaData);

        Assertions.assertThrows(UnsupportedOperationException.class, () ->
                extractor.put("key1", "value1"));
    }
}