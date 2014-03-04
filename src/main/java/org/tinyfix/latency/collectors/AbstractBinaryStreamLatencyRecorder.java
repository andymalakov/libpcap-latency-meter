package org.tinyfix.latency.collectors;

import org.tinyfix.latency.common.CaptureSettings;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractBinaryStreamLatencyRecorder extends OutputStreamLatencyRecorder {
    protected static final int SIZE_OF_INT = 8;
    protected final byte [] writeBuffer = new byte[SIZE_OF_INT];

    public AbstractBinaryStreamLatencyRecorder(String filename) throws IOException {
        this(new BufferedOutputStream(new FileOutputStream(filename), 8192));
    }

    public AbstractBinaryStreamLatencyRecorder(OutputStream os) throws IOException {
        super(os);
        assert CaptureSettings.MAX_CORRELATION_ID_LENGTH <= 256; // we fit correlation ID length into single byte
    }

    protected void writeLong(long v) throws IOException {
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

    protected static long readLong(byte[] readBuffer, int offset) throws IOException {
        return (((long)readBuffer[offset] << 56) +
                ((long)(readBuffer[offset+1] & 255) << 48) +
                ((long)(readBuffer[offset+2] & 255) << 40) +
                ((long)(readBuffer[offset+3] & 255) << 32) +
                ((long)(readBuffer[offset+4] & 255) << 24) +
                ((readBuffer[offset+5] & 255) << 16) +
                ((readBuffer[offset+6] & 255) <<  8) +
                ((readBuffer[offset+7] & 255)));
    }
}
