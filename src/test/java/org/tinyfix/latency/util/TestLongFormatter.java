package org.tinyfix.latency.util;

import org.junit.Test;
import org.tinyfix.latency.util.LongFormatter;

import static org.junit.Assert.assertEquals;

public class TestLongFormatter {


    @Test
    public void testSimple() {
        for(int i=-100; i < 100; i++)
            assertFormat(i);
    }

    @Test
    public void testExtremeCases() {
        assertFormat(Long.MIN_VALUE);
        assertFormat(Long.MAX_VALUE);
        assertFormat(Long.MIN_VALUE+1);
        assertFormat(Long.MAX_VALUE-1);
        assertFormat(Integer.MIN_VALUE);
        assertFormat(Integer.MAX_VALUE);
        assertFormat(0);
    }


    private static void assertFormat(long value) {
        String expectedFormat = Long.toString(value);

        byte [] buffer = new byte [20];
        int end = LongFormatter.format(value, buffer, 0);
        String actualFormat = new String (buffer, 0, end);
        assertEquals(expectedFormat, actualFormat);
    }
}
