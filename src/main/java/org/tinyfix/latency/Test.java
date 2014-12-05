package org.tinyfix.latency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.tinyfix.latency.common.CaptureSettings;

/** Slightly modified sample from jNetPcap to test that library is working fine */
public class Test {

    /**
     * Main startup method
     *
     * @param args
     *          ignored
     */
    public static void main(String[] args) throws Exception {
        //System.out.println ("Let's try a to load library directly...");
        //try {
        //    System.loadLibrary("jnetpcap");
        //} catch(Throwable ex){
        //    ex.printStackTrace();
        //}

        // Command line args
        int interfaceId = (args.length > 0) ? Integer.parseInt(args[0]) : 0; // We know we have at least 1 device
        String filterExpression = (args.length > 1) ? args[1] : null;

        List<PcapIf> alldevs = new ArrayList<>(); // Will be filled with NICs
        StringBuilder errbuf = new StringBuilder(); // For any error msgs

        /***************************************************************************
         * First get a list of devices on this system
         **************************************************************************/
        int r = Pcap.findAllDevs(alldevs, errbuf);
        if (r != Pcap.OK || alldevs.isEmpty()) {
            System.err.printf("Can't read list of devices, error is %s", errbuf
                    .toString());
            return;
        }

        System.out.println("Network devices found:");

        int i = 0;
        for (PcapIf device : alldevs) {
            String description = (device.getDescription() != null) ? device.getDescription() : "No description available";
            System.out.printf("#%d: %s [%s]\n", i++, device.getName(), description);
        }



        PcapIf device = alldevs.get(interfaceId);
        System.out.printf("\nChoosing '%s' interface (filter:%s)\n", (device.getDescription() != null) ? device.getDescription() : device.getName(), filterExpression);

        /***************************************************************************
         * Second we open up the selected device
         **************************************************************************/
        Pcap pcap = Pcap.openLive(device.getName(), CaptureSettings.PACKET_SNAP_LENGTH, Pcap.MODE_PROMISCUOUS, CaptureSettings.OPEN_LIVE_TIMEOUT_MILLIS, errbuf);

        if (pcap == null)
            throw new Exception ("Error while opening device for capture: " + errbuf.toString());

        if (filterExpression != null) {
            PcapBpfProgram program = new PcapBpfProgram();
            final int netmask = (int)Long.parseLong(CaptureSettings.FILTER_NETWORK_MASK_HEX.toLowerCase(), 16);
            if (pcap.compile(program, filterExpression, CaptureSettings.OPTIMIZE_FILTER ? 1 : 0, netmask) != Pcap.OK)
                throw new Exception("Error compiling LIBPCAP filter: " + pcap.getErr());

            if (pcap.setFilter(program) != Pcap.OK)
                throw new Exception("Error setting LIBPCAP filter: " + pcap.getErr());
        }

        /***************************************************************************
         * Third we create a packet handler which will receive packets from the
         * libpcap loop.
         **************************************************************************/
        PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {

            public void nextPacket(PcapPacket packet, String user) {

                System.out.printf("Received packet at %s caplen=%-4d len=%-4d %s\n",
                        new Date(packet.getCaptureHeader().timestampInMillis()),
                        packet.getCaptureHeader().caplen(),  // Length actually captured
                        packet.getCaptureHeader().wirelen(), // Original length
                        user                                 // User supplied object
                );
            }
        };

        /***************************************************************************
         * Fourth we enter the loop and tell it to capture 10 packets. The loop
         * method does a mapping of pcap.datalink() DLT value to JProtocol ID, which
         * is needed by JScanner. The scanner scans the packet buffer and decodes
         * the headers. The mapping is done automatically, although a variation on
         * the loop method exists that allows the programmer to sepecify exactly
         * which protocol ID to use as the data link type for this pcap interface.
         **************************************************************************/
        pcap.loop(10, jpacketHandler, "jNetPcap rocks!");

        /***************************************************************************
         * Last thing to do is close the pcap handle
         **************************************************************************/
        pcap.close();
    }
}