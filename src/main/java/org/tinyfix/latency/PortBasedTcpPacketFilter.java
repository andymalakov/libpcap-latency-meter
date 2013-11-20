package org.tinyfix.latency;

import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;

/**
* Packet filter that determines packet direction based on source/destination port numbers
*/
final class PortBasedTcpPacketFilter implements TcpPacketFilter {

    private final int inboundPort;
    private final int outboundPort;

    PortBasedTcpPacketFilter(int inboundPort, int outboundPort) {
        this.inboundPort = inboundPort;
        this.outboundPort = outboundPort;
    }

    @Override
    public Direction filter(Ip4 ip4, Tcp tcp) {
        if (tcp.source() == inboundPort)
            return Direction.Inbound;
        if (tcp.destination() == outboundPort)
            return Direction.Outbound;

        return Direction.Skip;
    }

    @Override
    public String toString() {
        return "Inbound port:" + inboundPort + "/Outbound port:" + outboundPort;
    }
}
