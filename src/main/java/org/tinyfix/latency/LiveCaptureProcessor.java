package org.tinyfix.latency;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapIf;
import org.tinyfix.latency.collectors.ChainedLatencyCollector;
import org.tinyfix.latency.collectors.CsvFileLatencyCollector;
import org.tinyfix.latency.collectors.LatencyCollector;
import org.tinyfix.latency.collectors.StatLatencyCollector;
import org.tinyfix.latency.util.ByteSequence2LongMap;
import org.tinyfix.latency.util.FixedSizeArrayTokenMap;

import java.util.ArrayList;
import java.util.List;

public class LiveCaptureProcessor {

    public static void main (String [] args) throws Exception {

        if (args.length == 0) {
            printNetworkInterfaces();
            System.exit(0);
        }

        int interfaceId = Integer.parseInt(args[0]);


        int inboundPort = Integer.parseInt(args[1]);
        int inboundToken = Integer.parseInt(args[2]); //FIX tag to watch for in the inbound traffic
        int outboundPort = Integer.parseInt(args[3]);
        int outboundToken = Integer.parseInt(args[4]);  //FIX tag to watch for in the outbound traffic

        PcapIf device = selectPcapIf(interfaceId);
        System.out.println("Recording from: " + device.getName() + " (" + device.getDescription() + ')');



        StringBuilder errbuf = new StringBuilder();
        int snaplen = 64 * 1024; // Capture all packets, no truncation
        int timeout = 10 * 1000; // 10 seconds in millis
        final Pcap pcap = Pcap.openLive(device.getName(), snaplen, Pcap.MODE_NON_PROMISCUOUS, timeout, errbuf);
        if (pcap == null)
            throw new IllegalArgumentException(errbuf.toString());


        final String filter = (args.length > 5) ? args[5] : null;
        if (filter != null)
            setupFilter(pcap, filter);

        final String outputFile = (args.length > 6) ? args[6] : "latencies.csv";
        int maxTokenLength = 32;

        int bufferSize = 4096*1024;
        final ByteSequence2LongMap timestampMap = new FixedSizeArrayTokenMap(bufferSize, maxTokenLength); // = new HashMapByteSequence2LongMap(bufferSize);

        System.out.println("Buffer size: " + bufferSize + 'b');

        final LatencyCollector latencyCollector = new ChainedLatencyCollector(
                new StatLatencyCollector(100, timestampMap),
                new CsvFileLatencyCollector(outputFile, maxTokenLength)
        );

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    System.err.println("Shutting down...");
                    pcap.breakloop();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });

        pcap.loop(-1,
                LatencyTestPacketHandler.create(
                        inboundPort, inboundToken,
                        outboundPort, outboundToken,
                        maxTokenLength,
                        latencyCollector,
                        timestampMap), null);

        pcap.close();
        latencyCollector.close();
        if (errbuf.length() > 0)
            System.err.println(errbuf.toString());
    }

    private static void setupFilter(Pcap pcap, String expression) throws Exception {
        PcapBpfProgram program = new PcapBpfProgram();
        int optimize = 0;         // 0 = false
        int netmask = 0xFFFFFF00; // 255.255.255.0
        if (pcap.compile(program, expression, optimize, netmask) != Pcap.OK)
            throw new Exception("Error compiling LIBPCAP filter: " + pcap.getErr());

        if (pcap.setFilter(program) != Pcap.OK)
            throw new Exception("Error seeting LIBPCAP filter: " + pcap.getErr());
    }

    private static PcapIf selectPcapIf(int interfaceId) throws Exception {
        List<PcapIf> interfaces = listNetworkInterfaces();
        if (interfaceId < 0 || interfaceId >= interfaces.size())
            throw new Exception("Invalid interface ID specified: " + interfaceId);
        return interfaces.get(interfaceId);
    }

    private static List<PcapIf> listNetworkInterfaces() throws Exception {
        final StringBuilder errbuf = new StringBuilder();
        List<PcapIf> result = new ArrayList<>(); // Will be filled with NICs
        int r = Pcap.findAllDevs(result, errbuf);
        if (r == Pcap.ERROR || result.isEmpty())
            throw new Exception ("Can't read list of devices, error is :" + errbuf.toString());

        return result;
    }


    private static void printNetworkInterfaces() throws Exception {
        List<PcapIf> interfaces = listNetworkInterfaces();
        System.out.println("Network devices found:");
        for (int i=0; i < interfaces.size(); i++) {
            PcapIf device = interfaces.get(i);
            String description = (device.getDescription() != null) ? device.getDescription() : "No description available";
            System.out.printf("#%d: %s [%s]\n", i++, device.getName(), description);
        }
    }
}
