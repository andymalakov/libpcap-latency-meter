package org.tinyfix.latency.collectors;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BinaryFileLatencyCollector implements LatencyCollector {
    private final OutputStream os;
    private final byte [] writeBuffer = new byte[8];

    public BinaryFileLatencyCollector(String filename, int maxTokenLength) throws IOException {
        this(new BufferedOutputStream(new FileOutputStream(filename), 8192), maxTokenLength);
    }

    public BinaryFileLatencyCollector(OutputStream os, int maxTokenLength) throws IOException {
        assert maxTokenLength < 256; // we fit token length into single byte
        this.os = os;
    }

    @Override
    public void recordLatency(byte[] buffer, int offset, int length, long latency) {
        assert length < 256;
        try {
            os.write(length);
            os.write(buffer, offset, length);
            writeLong (latency);
        } catch (IOException e) {
            throw new RuntimeException("Error writing latency stats", e);
        }
    }

    @Override
    public void missingInboundSignal(byte[] buffer, int offset, int length) {
    }

    private void writeLong(long v) throws IOException {
        writeBuffer[0] = (byte)(v >>> 56);
        writeBuffer[1] = (byte)(v >>> 48);
        writeBuffer[2] = (byte)(v >>> 40);
        writeBuffer[3] = (byte)(v >>> 32);
        writeBuffer[4] = (byte)(v >>> 24);
        writeBuffer[5] = (byte)(v >>> 16);
        writeBuffer[6] = (byte)(v >>>  8);
        writeBuffer[7] = (byte)(v >>>  0);
        os.write(writeBuffer, 0, 8);
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
