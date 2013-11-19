package org.tinyfix.latency.collectors;

import org.tinyfix.latency.util.LongFormatter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Similar to CsvFileLatencyCollector but instead of time difference it both timestamps as two separate columns */
public class CsvFileLatencyCollector2 extends OutputStreamLatencyRecorder {
    private static final int MAX_LENGTH_OF_DECIMAL_LONG = 20; // positive;
    private final byte [] lineBuffer;

    public CsvFileLatencyCollector2(String filename, int maxTokenLength) throws IOException {
        this(new BufferedOutputStream(new FileOutputStream(filename), 8192), maxTokenLength);
    }

    public CsvFileLatencyCollector2(OutputStream os, int maxTokenLength) throws IOException {
        super(os);
        this.lineBuffer = new byte [maxTokenLength + 2*(2 + MAX_LENGTH_OF_DECIMAL_LONG) + 1];
        os.write("Signal, Latency (us.)\n".getBytes());
    }

    @Override
    public synchronized void recordLatency(byte[] buffer, int offset, int length, long inboundTimestamp, long outboundTimestamp) {

        System.arraycopy(buffer, offset, lineBuffer, 0, length);

        int index = length;
        lineBuffer[index++] = ',';
        lineBuffer[index++] = ' ';

        index = LongFormatter.format(inboundTimestamp, lineBuffer, index);
        lineBuffer[index++] = ',';
        lineBuffer[index++] = ' ';
        index = LongFormatter.format(outboundTimestamp, lineBuffer, index);

        lineBuffer[index++] = '\n';

        try {
            os.write(lineBuffer, 0, index);
        } catch (IOException e) {
            throw new RuntimeException("Error writing latency stats", e);
        }
    }
}
