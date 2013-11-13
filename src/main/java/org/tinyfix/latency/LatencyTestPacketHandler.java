package org.tinyfix.latency;

import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.JPacketHandler;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.tinyfix.latency.collectors.LatencyCollector;
import org.tinyfix.latency.util.ByteSequence2LongMap;
import org.tinyfix.latency.util.FixedSizeArrayTokenMap;

class LatencyTestPacketHandler<T> implements JPacketHandler<T> {

    private final Tcp tcp = new Tcp();
    private final TcpPayloadHandler<T> [] handlers;

    LatencyTestPacketHandler(TcpPayloadHandler<T> ... handlers) {
        this.handlers = handlers;
    }

    @Override
    public void nextPacket(JPacket packet, T cookie) {
        if (packet.hasHeader(Ip4.ID) && packet.hasHeader(Tcp.ID)) {
            packet.getHeader(tcp);
            final int size = tcp.getPayloadLength();
            if (size > 0) {
                for (int i=0; i < handlers.length; i++)
                    handlers[i].nextPacket(packet, tcp, size, cookie);
            }
        }
    }

    static {
        System.err.println("TODO: DO NOT FORGET TO REMOVE HACK THAT TRIMS ZEROS (both inbound/outbound hacks)");
    }


    static <T> LatencyTestPacketHandler<T> create (final int inboundPort, final int inboundToken, final int outboundPort, final int outboundToken, final int maxTokenLength, final LatencyCollector latencyCollector) {
        final ByteSequence2LongMap timestampMap = new FixedSizeArrayTokenMap(1024*1024, maxTokenLength);
        //final ByteSequence2LongMap timestampMap = new HashMapByteSequence2LongMap();

        TcpPayloadHandler<T> inboundFlowHandler = new FilteredTcpPayloadHandler<>(
                new FilteredTcpPayloadHandler.TcpPacketFilter () {
                    public boolean accept(Tcp tcp) {
                        return tcp.source() == inboundPort;
                    }
                },

                new FixMessageTagExtractor<T>(inboundToken, maxTokenLength) {
                    public void tokenFound(JPacket packet, byte [] buffer, int offset, int length) {
                        long inboundTimestampUS = packet.getCaptureHeader().timestampInMicros();
                        timestampMap.put(buffer, offset, length, inboundTimestampUS); // assumes quote IDs are unique in scope of up to several seconds it may take to produce order
                    }
                }
            );

        TcpPayloadHandler<T> outboundFlowHandler = new FilteredTcpPayloadHandler<>(
                new FilteredTcpPayloadHandler.TcpPacketFilter () {
                    public boolean accept(Tcp tcp) {
                        return tcp.destination() == outboundPort;
                    }
                },
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
                }
            );

        return new LatencyTestPacketHandler<>(inboundFlowHandler, outboundFlowHandler);
    }
}
