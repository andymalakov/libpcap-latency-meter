package org.tinyfix.latency.collectors;

import org.tinyfix.latency.common.CaptureSettings;
import org.tinyfix.latency.util.TimeOfDayFormatter;

import java.io.*;
import java.util.Arrays;

/**
 * Records latency measurement in the following binary format format:
 * <pre>
 * OFFSET LENGTH DESCRIPTION
 * -- header--
 * 0        8     UTC timestamp of the moment we store this header (milliseconds count since 1/1/1970 0:00:00 UTC)
 * 8        8     Timestamp of the first inbound message (microseconds, result of KeQueryPerformanceCounter/1000)*
 * -- body --
 * 00       1     Size of correlation ID (N)
 * 01       N     Correlation ID (ASCII text)
 * N+1      8     Timestamp of inbound message (microseconds, result of KeQueryPerformanceCounter/1000)
 * N+9      8     Timestamp of outbound message (microseconds, result of KeQueryPerformanceCounter/1000)
 * </pre>
 * Main method formats results to the following CSV format:
 * <pre>time-of-day, correlation-id, latency (microseconds), timestamp of inbound, timestamp of outbound</pre>
 */


public class BinaryFileLatencyCollector2 extends AbstractBinaryStreamLatencyRecorder {
    private boolean headerStored;
    public BinaryFileLatencyCollector2(String filename) throws IOException {
        this(new BufferedOutputStream(new FileOutputStream(filename), 8192));
    }

    public BinaryFileLatencyCollector2(OutputStream os) throws IOException {
        super(os);
        assert CaptureSettings.MAX_CORRELATION_ID_LENGTH <= 256; // we fit correlation ID length into single byte
    }

    @Override
    public synchronized void recordLatency(byte[] buffer, int offset, int length, long inboundTimestamp, long outboundTimestamp) {
        assert length < 256; // must fit into byte
        try {
            if ( ! headerStored) {
                headerStored = true;
                writeLong (System.currentTimeMillis());
                writeLong (inboundTimestamp);
            }

            // correlationId (max length is 255 bytes)
            os.write(length);
            os.write(buffer, offset, length);
            writeLong (inboundTimestamp);
            writeLong (outboundTimestamp);
        } catch (IOException e) {
            throw new RuntimeException("Error writing latency stats", e);
        }
    }

    public static void main (String ...args) throws Exception {
        final String inputFile = args[0];
        final String outputFile = args[1];

        final int numberOfPoints = (args.length > 2) ? Integer.parseInt(args[2]) : 0;
        final int [] sortedLatencies = (numberOfPoints > 0) ? new int [numberOfPoints] : null;

        final char [] timestampBuffer = new char [TimeOfDayFormatter.FORMAT_LENGTH];
        final byte [] correlationIdBuffer = new byte [256 + 2*SIZE_OF_INT];
        final InputStream is = new FileInputStream(inputFile);
        final PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile), 8192));

        int signalCount = 0;
        try {
            // read 18-byte header
            is.read(correlationIdBuffer, 0 , 2*SIZE_OF_INT);
            final long epochTime = readLong(correlationIdBuffer, SIZE_OF_INT);
            final long queryPerformanceCounnterBase = readLong(correlationIdBuffer, SIZE_OF_INT);

            while (true) {
                int signalLength = is.read();
                if (signalLength == -1)
                    break; // EOF
                int totalRowSize = signalLength + 2*SIZE_OF_INT;
                int bytesRead = is.read(correlationIdBuffer, 0 , totalRowSize);
                if (bytesRead != totalRowSize) {
                    System.out.println("Unexpected EOF while reading signal #" + (signalCount+1));
                }

                long inboundTimestamp = readLong(correlationIdBuffer, signalLength);
                long outboundTimestamp = readLong(correlationIdBuffer, signalLength + SIZE_OF_INT);

                long timestamp = (outboundTimestamp-queryPerformanceCounnterBase)+epochTime;
                TimeOfDayFormatter.formatTimeOfDay(timestamp, timestampBuffer);
                writer.print(timestampBuffer);

                long latency = outboundTimestamp-inboundTimestamp;
                writer.print(',');
                writer.print(new String(correlationIdBuffer, 0, signalLength));
                writer.print(',');
                writer.print(latency);
                writer.print(',');
                writer.print(inboundTimestamp);
                writer.print(',');
                writer.print(outboundTimestamp);
                writer.print('\n');


                if (sortedLatencies != null) {
                    if (latency > Integer.MAX_VALUE)
                        throw new Exception("Latency value exceeds INT32: " + latency);
                    sortedLatencies[signalCount] = (int)latency;
                }

                signalCount++;
            }
        } finally {
            is.close();
            writer.close();

        }

        if (sortedLatencies != null && signalCount > 0) {

            System.out.println("Sorting " + signalCount + " results");
            Arrays.sort(sortedLatencies, 0, signalCount);
            System.out.println("MIN: " + sortedLatencies[0]);
            System.out.println("MAX: " + sortedLatencies[signalCount-1]);
            System.out.println("MEDIAN: " + sortedLatencies[signalCount/2]);

            System.out.println("99.000%: " + sortedLatencies[ (int)   (99L*signalCount/100)]);
            System.out.println("99.900%: " + sortedLatencies[ (int)  (999L*signalCount/1000)]);
            System.out.println("99.990%: " + sortedLatencies[ (int) (9999L*signalCount/10000)]);
            System.out.println("99.999%: " + sortedLatencies[ (int)(99999L*signalCount/100000)]);
        }


    }
}

