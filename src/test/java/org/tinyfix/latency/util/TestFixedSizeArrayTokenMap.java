package org.tinyfix.latency.util;

import junit.framework.Assert;
import org.junit.Test;
import org.tinyfix.latency.util.AsciiUtils;
import org.tinyfix.latency.util.FixedSizeArrayTokenMap;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestFixedSizeArrayTokenMap {

    private static final int maxCapacity = 8;
    private static final int maxKeyLength = 32;
    private FixedSizeArrayTokenMap m = new FixedSizeArrayTokenMap(maxCapacity, maxKeyLength);

    @Test
    public void testEmpty() {
        assertDumpEquals("H:-1 T:-1 {=0, =0, =0, =0, =0, =0, =0, =0}");
        assertContentEquals("");
        Assert.assertEquals(FixedSizeArrayTokenMap.NOT_FOUND, get("UNKNOWN"));
    }


    @Test
    public void testSingleFill() {
        Assert.assertEquals(FixedSizeArrayTokenMap.NOT_FOUND, get("UNKNOWN"));
        for (int i=0; i < maxCapacity; i++)
            put ("K"+i, i);

        assertContentEquals("K0=0, K1=1, K2=2, K3=3, K4=4, K5=5, K6=6, K7=7");
        for (int i=0; i < maxCapacity; i++)
            assertEquals ("Entry#" + i, i, get("K" + i));

        assertContentEquals(""); // empty
        Assert.assertEquals(FixedSizeArrayTokenMap.NOT_FOUND, get("UNKNOWN"));
    }

    @Test
    public void testUnknownValueConsumption() {
        for (int i=0; i < maxCapacity; i++)
            put ("K"+i, i);

        assertContentEquals("K0=0, K1=1, K2=2, K3=3, K4=4, K5=5, K6=6, K7=7");
        Assert.assertEquals(FixedSizeArrayTokenMap.NOT_FOUND, get("UNKNOWN"));
        assertContentEquals(""); // empty
    }

    @Test
    public void testOverFill() {
        for (int i=0; i < maxCapacity; i++)
            put ("K"+i, i);

        put("K" + maxCapacity, maxCapacity);
        put("K" + (maxCapacity+1), maxCapacity+1);
        assertDumpEquals("H:9 T:-1 {K8=8, K9=9, K2=2, K3=3, K4=4, K5=5, K6=6, K7=7}");
        assertContentEquals("K2=2, K3=3, K4=4, K5=5, K6=6, K7=7, K8=8, K9=9");

        assertEquals (2, get("K2"));
        assertDumpEquals("H:9 T:2 {K8=8, K9=9, K2=2, K3=3, K4=4, K5=5, K6=6, K7=7}");
        assertContentEquals("K3=3, K4=4, K5=5, K6=6, K7=7, K8=8, K9=9");

        assertEquals (3, get("K3"));

        assertDumpEquals("H:9 T:3 {K8=8, K9=9, K2=2, K3=3, K4=4, K5=5, K6=6, K7=7}");
        assertContentEquals("K4=4, K5=5, K6=6, K7=7, K8=8, K9=9");

        assertEquals (4, get("K4"));
        assertDumpEquals("H:9 T:4 {K8=8, K9=9, K2=2, K3=3, K4=4, K5=5, K6=6, K7=7}");
        assertContentEquals("K5=5, K6=6, K7=7, K8=8, K9=9");


        put("K" + (maxCapacity+2), maxCapacity+2);
        assertDumpEquals("H:10 T:4 {K8=8, K9=9, K10=10, K3=3, K4=4, K5=5, K6=6, K7=7}");
        assertContentEquals("K5=5, K6=6, K7=7, K8=8, K9=9, K10=10");

        assertEquals (5, get("K5"));
        assertDumpEquals("H:10 T:5 {K8=8, K9=9, K10=10, K3=3, K4=4, K5=5, K6=6, K7=7}");
        assertContentEquals("K6=6, K7=7, K8=8, K9=9, K10=10");

    }

    private void assertDumpEquals(String expected) {
        assertEquals("Dump match", expected, m.dump());
    }

    private void assertContentEquals(String expectedContent) {
        List<FixedSizeArrayTokenMap.BufferEntry> content = new ArrayList<>();
        m.snapshot(content);

        StringBuilder sb = new StringBuilder();
        for (FixedSizeArrayTokenMap.BufferEntry entry : content) {
            if (sb.length() != 0)
                sb.append(", ");
            sb.append(entry.toString());
        }

        assertEquals ("Content match", expectedContent, sb.toString());
    }

    private void put(String key, long value) {
        byte [] keyBuffer = AsciiUtils.getBytes(key);
        m.put(keyBuffer, 0, keyBuffer.length, value);
    }

    private long get (String key) {
        byte [] keyBuffer = AsciiUtils.getBytes(key);
        return m.get(keyBuffer, 0, keyBuffer.length);
    }

}
