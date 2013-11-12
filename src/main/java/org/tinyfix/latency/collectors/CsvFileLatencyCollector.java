package org.tinyfix.latency.collectors;

import org.tinyfix.latency.util.LongFormatter;

import java.io.*;

public class CsvFileLatencyCollector implements LatencyCollector {
    private static final int MAX_LENGTH_OF_DECIMAL_LONG = 20; // positive;
    private final OutputStream os;
    private final byte [] lineBuffer;

    public CsvFileLatencyCollector(String filename, int maxTokenLength) throws IOException {
        this(new BufferedOutputStream(new FileOutputStream(filename), 8192), maxTokenLength);
    }

    public CsvFileLatencyCollector(OutputStream os, int maxTokenLength) throws IOException {
        this.lineBuffer = new byte [maxTokenLength + 2 + MAX_LENGTH_OF_DECIMAL_LONG + 1];
        this.os = os;
        os.write("Signal, Latency (μs.)\n".getBytes());
    }

    @Override
    public void recordLatency(byte[] buffer, int offset, int length, long latency) {

        System.arraycopy(buffer, offset, lineBuffer, 0, length);

        int index = length;
        lineBuffer[index++] = ',';
        lineBuffer[index++] = ' ';

        index = LongFormatter.format(latency, lineBuffer, index);

        lineBuffer[index++] = '\n';

        try {
            os.write(lineBuffer, 0, index);
        } catch (IOException e) {
            throw new RuntimeException("Error writing latency stats", e);
        }
    }

    @Override
    public void missingInboundSignal(byte[] buffer, int offset, int length) {
    }

    @Override
    public void close() {
        try {
            os.close();
        } catch (IOException e) {
            throw new RuntimeException("Error writing latency stats", e);
        }
    }
}
