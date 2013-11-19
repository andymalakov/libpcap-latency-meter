package org.tinyfix.latency;

import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.JPacketHandler;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.tinyfix.latency.collectors.LatencyCollector;
import org.tinyfix.latency.util.ByteSequence2LongMap;

import java.net.Inet4Address;
import java.util.Arrays;

class LatencyTestPacketHandler<T> implements JPacketHandler<T> {

    private final TcpPacketFilter inboundPacketFilter;
    private final TcpPacketFilter outboundPacketFilter;
    private final CorrelationIdExtractor<T> inboundFlowHandler;
    private final CorrelationIdExtractor<T> outboundFlowHandler;

    private final Tcp tcp = new Tcp();
//    private final Ip4 ip = new Ip4();
//    byte [] localIpAddress;

    LatencyTestPacketHandler(TcpPacketFilter inboundPacketFilter, CorrelationIdExtractor<T> inbound, TcpPacketFilter outboundPacketFilter, CorrelationIdExtractor<T> outbound) {
        this.inboundPacketFilter = inboundPacketFilter;
        this.outboundPacketFilter = outboundPacketFilter;
        this.inboundFlowHandler = inbound;
        this.outboundFlowHandler = outbound;

//        try {
//            localIpAddress = Inet4Address.getLocalHost().getAddress();
//        } catch (Exception e) {
//            throw new RuntimeException("Can't resolve local IP address " + e.getMessage(), e);
//        }
    }

    @Override
    public void nextPacket(JPacket packet, T cookie) {
        if (packet.hasHeader(Ip4.ID) && packet.hasHeader(Tcp.ID)) {

            packet.getHeader(tcp);
            final int size = tcp.getPayloadLength();
            if (size > 0) {
//                if (Arrays.equals(localIpAddress, ip.destination())) {
                    if (inboundPacketFilter.accept(tcp))
                        inboundFlowHandler.parse(packet, tcp.getPayloadOffset(), size, cookie);
//                }
                else
//                if (Arrays.equals(localIpAddress, ip.source())) {
                    if (outboundPacketFilter.accept(tcp))
                       outboundFlowHandler.parse(packet, tcp.getPayloadOffset(), size, cookie);
//                }
            }
        }
    }

    static <T> LatencyTestPacketHandler<T> create (final int inboundPort, final int inboundToken, final int outboundPort, final int outboundToken, final int maxTokenLength, final LatencyCollector latencyCollector, final ByteSequence2LongMap timestampMap) {
        TcpPacketFilter inboundFlowFilter = new TcpPacketFilter () {
            public boolean accept(Tcp tcp) {
                return tcp.source() == inboundPort; // true if this is inbound packet
            }
        };

        TcpPacketFilter outboundFlowFilter = new TcpPacketFilter () {
            public boolean accept(Tcp tcp) {
                return tcp.destination() == outboundPort;  // true if this is outbound packet
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
                    long inboundTimestampUS = timestampMap.get(buffer, offset, length);

                    if (inboundTimestampUS != ByteSequence2LongMap.NOT_FOUND) {
                        long outboundTimestampUS = packet.getCaptureHeader().timestampInMicros();
                        latencyCollector.recordLatency(buffer, offset, length, inboundTimestampUS, outboundTimestampUS);
                    } else {
                        latencyCollector.missingInboundSignal(buffer, offset, length);
                    }
                }
            };

        return new LatencyTestPacketHandler<>(inboundFlowFilter, inboundFlowHandler, outboundFlowFilter, outboundFlowHandler);
    }
}
