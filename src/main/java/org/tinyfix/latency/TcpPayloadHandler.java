package org.tinyfix.latency;

import org.jnetpcap.packet.JPacket;
import org.jnetpcap.protocol.tcpip.Tcp;

public interface TcpPayloadHandler<T> {
    void nextPacket(JPacket packet, Tcp tcp, int size, T cookie);
}
