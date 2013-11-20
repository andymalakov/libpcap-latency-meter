package org.tinyfix.latency;

import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;

interface TcpPacketFilter {
    public enum Direction {
        Inbound,
        Outbound,
        Skip
    }

    Direction filter(Ip4 ip4, Tcp tcp);
}
