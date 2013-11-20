package org.tinyfix.latency;

import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.JPacketHandler;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.tinyfix.latency.collectors.LatencyCollector;
import org.tinyfix.latency.protocols.CorrelationIdExtractor;
import org.tinyfix.latency.protocols.CorrelationIdListener;
import org.tinyfix.latency.util.ByteSequence2LongMap;

import java.net.Inet4Address;
import java.util.Arrays;

class LatencyMeterPacketHandler<T> implements JPacketHandler<T> {


    private final TcpPacketFilter packetFilter;
    private final CorrelationIdExtractor<T> inboundFlowHandler;
    private final CorrelationIdExtractor<T> outboundFlowHandler;

    private final Tcp tcp = new Tcp();
    private final Ip4 ip4 = new Ip4();

    LatencyMeterPacketHandler(TcpPacketFilter packetFilter, CorrelationIdExtractor<T> inbound, CorrelationIdExtractor<T> outbound) {
        this.packetFilter = packetFilter;
        this.inboundFlowHandler = inbound;
        this.outboundFlowHandler = outbound;

    }

    @Override
    public void nextPacket(JPacket packet, T cookie) {
        if (packet.hasHeader(Ip4.ID) && packet.hasHeader(Tcp.ID)) {

            packet.getHeader(ip4);
            packet.getHeader(tcp);

            final int payloadLength = tcp.getPayloadLength();
            if (payloadLength > 0) {
                switch (packetFilter.filter(ip4, tcp)) {
                    case Inbound:
                        inboundFlowHandler.parse(packet, tcp.getPayloadOffset(), payloadLength, cookie);
                        break;
                    case Outbound:
                        outboundFlowHandler.parse(packet, tcp.getPayloadOffset(), payloadLength, cookie);
                        break;
                    case Skip:
                        break;
                }
            }
        }
    }

}
