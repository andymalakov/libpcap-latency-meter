package org.tinyfix.latency;

import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
* Packet filter that determines packet direction based on IP v4 host address
*/
final class HostBasedPacketFilter implements TcpPacketFilter {
    private final byte [] localIpAddress;

    HostBasedPacketFilter() {
        try {
            localIpAddress = Inet4Address.getLocalHost().getAddress();
        } catch (Exception e) {
            throw new RuntimeException("Can't resolve local IP address " + e.getMessage(), e);
        }
    }

    HostBasedPacketFilter(String address) throws UnknownHostException {
        this.localIpAddress = Inet4Address.getByName(address).getAddress();
    }

    HostBasedPacketFilter(byte[] localIpAddress) {
        this.localIpAddress = localIpAddress;
    }

    @Override
    public Direction filter(Ip4 ip4, Tcp tcp) {
        if (Arrays.equals(localIpAddress, ip4.destination()))
            return Direction.Inbound;
        if (Arrays.equals(localIpAddress, ip4.source()))
            return Direction.Outbound;

        return Direction.Skip;
    }

    @Override
    public String toString() {
        return "Inbound/outbound direction relative to " + (0xFF&localIpAddress[0]) + '.' + (0xFF&localIpAddress[1]) + '.' + (0xFF&localIpAddress[2]) + '.' + (0xFF&localIpAddress[3]);
    }
}
