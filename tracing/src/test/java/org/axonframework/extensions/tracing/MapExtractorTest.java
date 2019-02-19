package org.axonframework.extensions.tracing;

import org.axonframework.messaging.MetaData;
import org.junit.*;

import java.util.Iterator;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class MapExtractorTest {

    @Test
    public void testExtractor() {
        MetaData metaData = MetaData.emptyInstance();
        metaData = metaData.and("key", "value");

        MapExtractor extractor = new MapExtractor(metaData);
        Iterator<Map.Entry<String, String>> iterator = extractor.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getValue(), is("value"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupported() {
        MetaData metaData = MetaData.emptyInstance();
        metaData = metaData.and("key", "value");

        MapExtractor extractor = new MapExtractor(metaData);
        extractor.put("key1", "value1");
    }
}