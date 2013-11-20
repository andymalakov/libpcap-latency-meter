package org.tinyfix.latency.collectors;

import org.tinyfix.latency.common.CaptureSettings;
import org.tinyfix.latency.util.LongFormatter;

import java.io.*;

public class CsvFileLatencyCollector extends OutputStreamLatencyRecorder {
    private static final int MAX_LENGTH_OF_DECIMAL_LONG = 20; // positive;
    private final byte [] lineBuffer;

    public CsvFileLatencyCollector(String filename) throws IOException {
        this(new BufferedOutputStream(new FileOutputStream(filename), 8192));
    }

    public CsvFileLatencyCollector(OutputStream os) throws IOException {
        super(os);
        this.lineBuffer = new byte [CaptureSettings.MAX_CORRELATION_ID_LENGTH + 2 + MAX_LENGTH_OF_DECIMAL_LONG + 1];
        os.write("Signal, Latency (us.)\n".getBytes());
    }

    @Override
    public synchronized void recordLatency(byte[] buffer, int offset, int length, long inboundTimestamp, long outboundTimestamp) {

        System.arraycopy(buffer, offset, lineBuffer, 0, length);

        int index = length;
        lineBuffer[index++] = ',';
        lineBuffer[index++] = ' ';

        index = LongFormatter.format(outboundTimestamp - inboundTimestamp, lineBuffer, index);

        lineBuffer[index++] = '\n';

        try {
            os.write(lineBuffer, 0, index);
        } catch (IOException e) {
            throw new RuntimeException("Error writing latency stats", e);
        }
    }

}
