package org.tinyfix.latency;

import org.jnetpcap.protocol.tcpip.Tcp;

interface TcpPacketFilter {
    boolean accept (Tcp tcp);
}
