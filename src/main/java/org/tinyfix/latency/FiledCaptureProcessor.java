package org.tinyfix.latency;

import org.jnetpcap.Pcap;
import org.tinyfix.latency.collectors.ChainedLatencyCollector;
import org.tinyfix.latency.collectors.CsvFileLatencyCollector;
import org.tinyfix.latency.collectors.LatencyCollector;
import org.tinyfix.latency.collectors.StatLatencyCollector;

public class FiledCaptureProcessor {

    private static void printHelp() {
        System.out.println("Command line arguments: <filename> <inboundPort> <inboundFixTag> <outboundPort> <outboundFixTag>");
    }

    public static void main (String [] args) throws Exception {
        if (args.length == 0) {
            printHelp();
            System.exit(-1);
        }

        String filename = args[0];

        int inboundPort = Integer.parseInt(args[1]);
        int inboundToken = Integer.parseInt(args[2]); //FIX tag to watch for in the inbound traffic
        int outboundPort = Integer.parseInt(args[3]);
        int outboundToken = Integer.parseInt(args[4]);  //FIX tag to watch for in the outbound traffic

        StringBuilder errbuf = new StringBuilder();
        Pcap pcap = Pcap.openOffline(filename, errbuf);
        if (pcap == null)
            throw new IllegalArgumentException(errbuf.toString());

        int maxTokenLength = 32;

        LatencyCollector latencyCollector = new ChainedLatencyCollector(
            new StatLatencyCollector(4096),
            new CsvFileLatencyCollector("latencies.csv", maxTokenLength)
        );


        pcap.loop(-1,
            LatencyTestPacketHandler.create(
                inboundPort, inboundToken,
                outboundPort, outboundToken,
                maxTokenLength,
                latencyCollector), null);

        latencyCollector.close();

        pcap.close();
        if (errbuf.length() > 0)
            System.err.println(errbuf.toString());
    }

}
