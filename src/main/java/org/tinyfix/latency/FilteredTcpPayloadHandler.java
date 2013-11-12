package org.tinyfix.latency;

import org.jnetpcap.packet.JPacket;
import org.jnetpcap.protocol.tcpip.Tcp;

public class FilteredTcpPayloadHandler<T> implements TcpPayloadHandler<T> {

    interface TcpPacketFilter {
        boolean accept (Tcp tcp);
    }

    private final TcpPacketFilter filter;
    private final CorrelationIdExtractor<T> idExtractor;


    public FilteredTcpPayloadHandler(TcpPacketFilter filter, CorrelationIdExtractor<T> idExtractor) {
        this.filter = filter;
        this.idExtractor = idExtractor;
    }

    @Override
    public void nextPacket(JPacket packet, Tcp tcp, int size, T cookie) {
        if (filter.accept(tcp))
            idExtractor.parse(packet, tcp.getPayloadOffset(), size, cookie);
    }




}
