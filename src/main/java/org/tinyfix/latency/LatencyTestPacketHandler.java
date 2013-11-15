package org.tinyfix.latency;

import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.JPacketHandler;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.tinyfix.latency.collectors.LatencyCollector;
import org.tinyfix.latency.util.ByteSequence2LongMap;

class LatencyTestPacketHandler<T> implements JPacketHandler<T> {

    private final TcpPacketFilter inboundPacketFilter;
    private final CorrelationIdExtractor<T> inboundFlowHandler;
    private final CorrelationIdExtractor<T> outboundFlowHandler;

    private final Tcp tcp = new Tcp();

    LatencyTestPacketHandler(TcpPacketFilter inboundPacketFilter, CorrelationIdExtractor<T> inbound, CorrelationIdExtractor<T> outbound) {
        this.inboundPacketFilter = inboundPacketFilter;
        this.inboundFlowHandler = inbound;
        this.outboundFlowHandler = outbound;
    }

    @Override
    public void nextPacket(JPacket packet, T cookie) {
        if (packet.hasHeader(Ip4.ID) && packet.hasHeader(Tcp.ID)) {
            packet.getHeader(tcp);
            final int size = tcp.getPayloadLength();
            if (size > 0) {
                if (inboundPacketFilter.accept(tcp))
                    inboundFlowHandler.parse(packet, tcp.getPayloadOffset(), size, cookie);
                else
                    outboundFlowHandler.parse(packet, tcp.getPayloadOffset(), size, cookie);
            }
        }
    }

    static <T> LatencyTestPacketHandler<T> create (final int inboundPort, final int inboundToken, final int outboundPort, final int outboundToken, final int maxTokenLength, final LatencyCollector latencyCollector, final ByteSequence2LongMap timestampMap) {
        TcpPacketFilter inboundFlowFilter = new TcpPacketFilter () {
            public boolean accept(Tcp tcp) {
                return tcp.source() == inboundPort;
            }
        };

        CorrelationIdExtractor<T> inboundFlowHandler =
            new FixMessageTagExtractor<T>(inboundToken, maxTokenLength) {
                public void tokenFound(JPacket packet, byte [] buffer, int offset, int length) {
                    long inboundTimestampUS = packet.getCaptureHeader().timestampInMicros();
                    timestampMap.put(buffer, offset, length, inboundTimestampUS); // assumes quote IDs are unique in scope of up to several seconds it may take to produce order
                }
            };

        CorrelationIdExtractor<T> outboundFlowHandler =
            new FixMessageTagExtractor<T>(outboundToken, maxTokenLength) {
                public void tokenFound(JPacket packet, byte [] buffer, int offset, int length) {
                    long outboundTimestampUS = packet.getCaptureHeader().timestampInMicros();
                    long inboundTimestampUS = timestampMap.get(buffer, offset, length);

                    if (inboundTimestampUS != ByteSequence2LongMap.NOT_FOUND) {
                        latencyCollector.recordLatency(buffer, offset, length, outboundTimestampUS - inboundTimestampUS);
                    } else {
                        latencyCollector.missingInboundSignal(buffer, offset, length);
                    }
                }
            };

        return new LatencyTestPacketHandler<>(inboundFlowFilter, inboundFlowHandler, outboundFlowHandler);
    }
}
