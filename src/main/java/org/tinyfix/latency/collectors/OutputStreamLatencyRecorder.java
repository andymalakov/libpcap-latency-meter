package org.tinyfix.latency.collectors;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class OutputStreamLatencyRecorder implements LatencyCollector {
    protected final OutputStream os;

    public OutputStreamLatencyRecorder(String filename) throws IOException {
        this(new BufferedOutputStream(new FileOutputStream(filename), 8192));
    }

    public OutputStreamLatencyRecorder(OutputStream os) throws IOException {
        this.os = os;
    }


    @Override
    public void missingInboundSignal(byte[] buffer, int offset, int length) {
        // by default do nothing (Stat printer will log on screen)
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
