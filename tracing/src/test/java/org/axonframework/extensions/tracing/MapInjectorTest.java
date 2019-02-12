package org.axonframework.extensions.tracing;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;

import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class MapInjectorTest {

    @Test
    public void testInject() {
        MapInjector injector = new MapInjector();
        injector.put("key", "value");
        Map<String, String> metaData = injector.getMetaData();
        assertThat(metaData.get("key"), CoreMatchers.is("value"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupported() {
        MapInjector injector = new MapInjector();
        injector.iterator();
    }
}