package org.tinyfix.latency;

import org.jnetpcap.Pcap;
import org.jnetpcap.packet.JPacket;
import org.tinyfix.latency.collectors.*;
import org.tinyfix.latency.common.CaptureSettings;
import org.tinyfix.latency.protocols.CorrelationIdExtractor;
import org.tinyfix.latency.protocols.CorrelationIdListener;
import org.tinyfix.latency.protocols.ProtocolHandlerFactory;
import org.tinyfix.latency.protocols.ProtocolHandlers;
import org.tinyfix.latency.util.ByteSequence2LongMap;
import org.tinyfix.latency.util.KeyValueRingBuffer;

import java.io.IOException;

public class AbstractCaptureProcessor<T> {
    protected String outputCsvFile = "latencies.csv";
    protected TcpPacketFilter packetFilter = new HostBasedPacketFilter();
    protected ProtocolHandlerFactory<T> inboundHandler;
    protected ProtocolHandlerFactory<T> outboundHandler;

    protected void run (String ... args) throws Exception {
        parse(args);
    }

    protected void parse(String ... args) {
        if (args.length == 0) {
            printHelp();
            System.exit(1);
        } else {
            for (String arg : args) {
                if ( ! parseCommandLineArgument (arg))
                    throw new IllegalArgumentException ("Unrecognized command line parameter: " + arg);
            }
        }
    }

    protected void printHelp () {
        System.out.println("COMMAND LINE ARGUMENTS:");
        System.out.println("\t-in:<protocol-handler>\t- Specifies inbound protocol handler. For example: -in:timebase ");
        System.out.println("\t-out:<protocol-handler>\t- Specifies outbound protocol handler. For example: -out:fix:299");
        System.out.println("\t-csv:filename\t- Specifies file name of output file will latencies stats. [Optional]");
    }

    protected boolean parseCommandLineArgument (String arg) {
        if (arg.startsWith("-dir:")) {
            String [] ports = value(arg).split(":");
            packetFilter = new PortBasedTcpPacketFilter(Integer.parseInt(ports[0]), Integer.parseInt(ports[1]));
            return true;
        }
        if (arg.startsWith("-in:")) {
            inboundHandler = ProtocolHandlers.getProtocolHandler(value(arg));
            return true;
        }
        if (arg.startsWith("-out:")) {
            outboundHandler = ProtocolHandlers.getProtocolHandler(value(arg));
            return true;
        }
        if (arg.startsWith("-csv:")) {
            outputCsvFile = value(arg);
            return true;
        }

        return false;
    }

    private volatile boolean isClosed;

    protected void runCaptureLoop(final Pcap pcap, final StringBuilder err) throws IOException {
        final ByteSequence2LongMap timestampMap = new KeyValueRingBuffer(CaptureSettings.RING_BUFFER_CAPACITY, CaptureSettings.MAX_CORRELATION_ID_LENGTH); // = new HashMapByteSequence2LongMap(bufferSize);
        System.out.println("Packet direction filter: " + packetFilter);
        System.out.println("Inbound packet handler: " + inboundHandler);
        System.out.println("Outbound packet handler: " + outboundHandler);
        System.out.println("Inbound signals buffer size: " + CaptureSettings.RING_BUFFER_CAPACITY/1024 + 'K');
        System.out.println("Capture packet snap length: " + CaptureSettings.PACKET_SNAP_LENGTH/1024 + 'K');
        System.out.println("Capture filter network mask: " + CaptureSettings.FILTER_NETWORK_MASK_HEX);

        final LatencyCollector latencyCollector = createLatencyCollector(timestampMap);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    System.err.println("Shutting down...");
                    if (!isClosed)
                        pcap.breakloop();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            pcap.loop(-1, createLatencyMeterPacketHandler(latencyCollector, timestampMap), null);
        } finally {
            pcap.close();
            latencyCollector.close();
            isClosed = true;
        }

        if (err.length() > 0)
            System.err.println(err.toString());
    }


    protected LatencyCollector createLatencyCollector(ByteSequence2LongMap timestampMap) throws IOException {
        return new ChainedLatencyCollector(
                new StatLatencyCollector(100, timestampMap),
                CaptureSettings.DUMP_TIMESTAMPS ? new CsvFileLatencyCollector2(outputCsvFile) : new CsvFileLatencyCollector(outputCsvFile)
        );
    }

    LatencyMeterPacketHandler<T> createLatencyMeterPacketHandler (final LatencyCollector latencyCollector, final ByteSequence2LongMap timestampMap) {
        CorrelationIdListener inboundIdListener = new CorrelationIdListener() {
            public void onCorrelationId(JPacket packet, byte[] buffer, int offset, int length) {
                long inboundTimestampUS = packet.getCaptureHeader().timestampInMicros();
                timestampMap.put(buffer, offset, length, inboundTimestampUS); // assumes quote IDs are unique in scope of up to several seconds it may take to produce order
            }
        };

        CorrelationIdListener outboundIdListener = new CorrelationIdListener() {
            public void onCorrelationId(JPacket packet, byte[] buffer, int offset, int length) {
                long inboundTimestampUS = timestampMap.get(buffer, offset, length);

                if (inboundTimestampUS != ByteSequence2LongMap.NOT_FOUND) {
                    long outboundTimestampUS = packet.getCaptureHeader().timestampInMicros();
                    latencyCollector.recordLatency(buffer, offset, length, inboundTimestampUS, outboundTimestampUS);
                } else {
                    latencyCollector.missingInboundSignal(buffer, offset, length);
                }
            }
        };

        CorrelationIdExtractor<T> inboundIdExtractor = inboundHandler.create(inboundIdListener);
        CorrelationIdExtractor<T> outboundIdExtractor = outboundHandler.create(outboundIdListener);

        return new LatencyMeterPacketHandler<>(packetFilter, inboundIdExtractor, outboundIdExtractor);
    }


    protected static String value (String arg) {
        return arg.substring(arg.indexOf(':') + 1);
    }
}
