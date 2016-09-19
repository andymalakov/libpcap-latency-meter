package org.tinyfix.latency;

import org.jnetpcap.Pcap;
import org.jnetpcap.packet.JPacket;
import org.tinyfix.latency.collectors.*;
import org.tinyfix.latency.common.CaptureSettings;
import org.tinyfix.latency.protocols.*;
import org.tinyfix.latency.util.ByteSequence2LongMap;
import org.tinyfix.latency.util.KeyValueRingBuffer;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class AbstractCaptureProcessor<T> {
    private static final boolean trimLeadingZeros = Boolean.getBoolean("correlationIdTrimLeadingZeros");
    protected String outputFile = "latencies.csv";
    protected TcpPacketFilter packetFilter;
    protected String inboundHandlerKey;
    protected String outboundHandlerKey;
    protected int statBufferSize = 100;
    protected boolean isTracingMode = false;

    protected final ProtocolHandlers<T> protocolHandlers = new ProtocolHandlers<>();

    protected void run (String ... args) throws Exception {
        parse(args);
    }

    protected void parse(String ... args) throws Exception {
        if (args.length == 0) {
            printHelp();
            System.exit(1);
        } else {
            for (String arg : args) {
                if ( ! parseCommandLineArgument (arg))
                    throw new IllegalArgumentException ("Unrecognized command line parameter: " + arg);
            }

            // assign defaults
            if(packetFilter == null) {
                packetFilter = new HostBasedPacketFilter();
            }
        }
    }

    protected void printHelp () throws Exception {
        System.out.println("COMMAND LINE ARGUMENTS:");
        System.out.println("\t-in:<protocol-handler>\t- Specifies inbound protocol handler. For example: -in:timebase ");
        System.out.println("\t-out:<protocol-handler>\t- Specifies outbound protocol handler. For example: -out:fix:299");
        System.out.println("\t-dir:<port1>:<port2>\t- When specified helps determine packet direction (otherwise direction is determined relative to local host)");
        System.out.println("\t-dir:<host> alternative form of packet direction - relative to specified host (the argument can be dnsname or ip address)");
        System.out.println("\t-csv:filename\t- Specifies file name of output file will latencies stats. [Optional]");
        System.out.println("\t-stat:N\t- Specifies number of outbound signal signals to display console progress. [Optional]");
        System.out.println("\t-trace\t- Traces all discovered inbound and outbound signals. [Slow]");
        System.out.println("\t-p:<factory-class>\t- Custom implementation of CorrelationIdExtractorFactory");
    }

    protected boolean parseCommandLineArgument (String arg) {
        if (arg.startsWith("-dir:")) {
            String value = value(arg);
            if (value.indexOf(':') > 0) {// looks like ports
                String [] ports = value.split(":");
                packetFilter = new PortBasedTcpPacketFilter(Integer.parseInt(ports[0]), Integer.parseInt(ports[1]));
            } else {
                try {
                    packetFilter = new HostBasedPacketFilter(value);
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException("Host specified for -dir parameter is not valid: "+ e.getMessage(), e);
                }
            }
            return true;
        }
        if (arg.startsWith("-p:")) {
            String protocolClassName = arg.substring(3);
            protocolHandlers.addProtocolHandler(protocolClassName);
            return true;
        }
        if (arg.startsWith("-in:")) {
            inboundHandlerKey = value(arg);
            return true;
        }
        if (arg.startsWith("-out:")) {
            outboundHandlerKey = value(arg);
            return true;
        }
        if (arg.startsWith("-result:")) {
            outputFile = value(arg);
            return true;
        }
        if (arg.startsWith("-csv:")) {
            System.err.println("Argument -csv:filename has been depracated by -result:filename");
            outputFile = value(arg);
            return true;
        }
        if (arg.startsWith("-stat:")) {
            statBufferSize = Integer.parseInt(value(arg));
            return true;
        }
        if (arg.startsWith("-trace")) {
            isTracingMode = true;
            return true;
        }

        return false;
    }

    private volatile boolean isClosed;

    protected void runCaptureLoop(final Pcap pcap, final StringBuilder err) throws IOException {
        final ByteSequence2LongMap timestampMap = new KeyValueRingBuffer(CaptureSettings.RING_BUFFER_CAPACITY, CaptureSettings.MAX_CORRELATION_ID_LENGTH); // = new HashMapByteSequence2LongMap(bufferSize);

        printSelectedSettings();

        final LatencyCollector latencyCollector = createLatencyCollector(timestampMap);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    System.err.println("Shutting down...");
                    if (!isClosed)
                        pcap.breakloop();
                    latencyCollector.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });


        LatencyMeterPacketHandler<T> handler = createLatencyMeterPacketHandler(latencyCollector, timestampMap);
        try {
            pcap.loop(-1, handler, null);
        } finally {
            pcap.close();
            latencyCollector.close();
            isClosed = true;
        }

        if (err.length() > 0)
            System.err.println(err.toString());
    }

    protected void printSelectedSettings() {
        System.out.println("Packet direction filter: " + packetFilter);
        System.out.println("Inbound packet handler: " + inboundHandlerKey);
        System.out.println("Outbound packet handler: " + outboundHandlerKey);
        System.out.println("Inbound signals buffer size: " + CaptureSettings.RING_BUFFER_CAPACITY/1024 + 'K');
        System.out.println("Capture packet snap length: " + CaptureSettings.PACKET_SNAP_LENGTH/1024 + 'K');
        System.out.println("Capture filter network mask: " + CaptureSettings.FILTER_NETWORK_MASK_HEX);
        System.out.println("Tracing: " + isTracingMode);
    }

    protected LatencyCollector createLatencyCollector(ByteSequence2LongMap timestampMap) throws IOException {
        List<LatencyCollector> result = new ArrayList<>();
        if (statBufferSize > 0)
            result.add(new StatLatencyCollector(statBufferSize, timestampMap));

        if (outputFile.toLowerCase().endsWith(".csv")) {
            if (CaptureSettings.DUMP_TIMESTAMPS)
                result.add(new CsvFileLatencyCollector2(outputFile));
            else
                result.add(new CsvFileLatencyCollector(outputFile));
        } else if (outputFile.toLowerCase().endsWith(".hlog")){
            result.add(new HdrHistLatencyCollector(outputFile));
        } else {
            if (outputFile.toLowerCase().endsWith(".bin2"))
                result.add(new BinaryFileLatencyCollector2(outputFile));
            else
                result.add(new BinaryFileLatencyCollector(outputFile));
        }

        return new ChainedLatencyCollector(result.toArray(new LatencyCollector[result.size()]));
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

        if (isTracingMode) {
            inboundIdListener =  new TracingCorrelationIdListener("IN>  ", inboundIdListener);
            outboundIdListener = new TracingCorrelationIdListener("OUT> ", outboundIdListener);
        }

        if (trimLeadingZeros) {
            inboundIdListener = new TrimmingCorrelationIdListener(inboundIdListener);
            outboundIdListener = new TrimmingCorrelationIdListener(outboundIdListener);
        }

        CorrelationIdExtractor<T> inboundIdExtractor = protocolHandlers.create(inboundHandlerKey, inboundIdListener);
        CorrelationIdExtractor<T> outboundIdExtractor = protocolHandlers.create(outboundHandlerKey, outboundIdListener);

        return new LatencyMeterPacketHandler<>(packetFilter, inboundIdExtractor, outboundIdExtractor);
    }


    protected static String value (String arg) {
        return arg.substring(arg.indexOf(':') + 1);
    }

    /** Trim leading zeros */
    private static final class TrimmingCorrelationIdListener implements CorrelationIdListener {
        private final CorrelationIdListener delegate;

        private TrimmingCorrelationIdListener(CorrelationIdListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onCorrelationId(JPacket packet, byte[] buffer, int offset, int length) {
            while (length > 0 && buffer[offset] == '0') {
                offset++; length--;
            }
            delegate.onCorrelationId(packet, buffer, offset, length);
        }
    }
}
