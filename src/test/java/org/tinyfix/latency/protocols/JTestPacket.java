package org.tinyfix.latency.protocols;

import org.jnetpcap.JCaptureHeader;
import org.jnetpcap.packet.JPacket;

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
    public String toString() {
        return Arrays.toString(payload);
    }
}
