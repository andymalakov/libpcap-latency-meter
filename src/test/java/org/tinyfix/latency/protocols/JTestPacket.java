package org.tinyfix.latency.protocols;

import org.jnetpcap.JCaptureHeader;
import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.packet.JPacket;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
* @author Andy
*         Date: 2/5/14
*/
class JTestPacket extends JPacket {

    final byte [] payload;

    public JTestPacket(byte[] payload) {
        super(Type.POINTER);
        this.payload = payload;
    }

    @Override
    public byte getByte(int index) {
        return payload[index];
    }

    @Override
    public JCaptureHeader getCaptureHeader() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getTotalSize() {
        throw new UnsupportedOperationException();
    }

    public int getLength() {
        return payload.length;
    }

    @Override
    public int transferTo(JBuffer dst, int srcOffset, int length, int dstOffset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int transferTo(JBuffer dst) {
        return dst.transferFrom(payload);
    }

    @Override
    public int transferTo(ByteBuffer dst, int srcOffset, int length) {
        dst.put(payload, srcOffset, length);
        return length;
    }

    @Override
    public String toString() {
        return Arrays.toString(payload);
    }
}
