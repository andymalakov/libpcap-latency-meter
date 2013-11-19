package org.tinyfix.latency;

import org.junit.Test;
import org.tinyfix.latency.collectors.CsvFileLatencyCollector;
import org.tinyfix.latency.util.AsciiUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestFileLatencyCollector {

    @Test
    public void testSimple () throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        CsvFileLatencyCollector flc = new CsvFileLatencyCollector(baos, 32);
        for (int i=0; i < 3; i++) {
            byte [] token = AsciiUtils.getBytes("TOKEN" + i);
            flc.recordLatency(token, 0, token.length, 100000+i, 200000+i);
        }
        flc.close();


        byte [] actualContent = baos.toByteArray();

        assertEquals("Signal, Latency (us.)\nTOKEN0,               100000\nTOKEN1,               100001\nTOKEN2,               100002\n", new String (actualContent));
    }
}
