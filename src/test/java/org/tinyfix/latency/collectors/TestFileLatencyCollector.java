package org.tinyfix.latency.collectors;

import org.junit.Test;
import org.tinyfix.latency.collectors.CsvFileLatencyCollector;
import org.tinyfix.latency.util.AsciiUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestFileLatencyCollector {

    @Test
    public void testOneColumn () throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        CsvFileLatencyCollector flc = new CsvFileLatencyCollector(baos);
        for (int i=0; i < 3; i++) {
            byte [] correlationID = AsciiUtils.getBytes("TOKEN" + i);
            flc.recordLatency(correlationID, 0, correlationID.length, 0, 100000+i);
        }
        flc.close();


        byte [] actualContent = baos.toByteArray();

        assertEquals("Signal, Latency (us.)\nTOKEN0,               100000\nTOKEN1,               100001\nTOKEN2,               100002\n", new String (actualContent));
    }

    @Test
    public void testTwoColumns() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        CsvFileLatencyCollector2 flc = new CsvFileLatencyCollector2(baos);
        for (int i=0; i < 3; i++) {
            byte [] correlationID = AsciiUtils.getBytes("TOKEN" + i);
            flc.recordLatency(correlationID, 0, correlationID.length, 100000+i, 200000+i);
        }
        flc.close();


        byte [] actualContent = baos.toByteArray();

        assertEquals("Signal, In (us.), Out (us.)\nTOKEN0,               100000,               200000\nTOKEN1,               100001,               200001\nTOKEN2,               100002,               200002\n", new String (actualContent));
    }
}
