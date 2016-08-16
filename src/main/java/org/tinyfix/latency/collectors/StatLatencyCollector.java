package org.tinyfix.latency.collectors;

import org.tinyfix.latency.util.ByteSequence2LongMap;
import org.tinyfix.latency.util.TimeOfDayFormatter;

public class StatLatencyCollector implements LatencyCollector {
    private final ByteSequence2LongMap signalsBuffer;
    private final char [] timestampBuffer = new char [TimeOfDayFormatter.FORMAT_LENGTH];
    private final long [] window;
    private long min, max, sum;
    private int index;
    private long count;

    private static final int MAX_INITIAL_SIGNALS_BEFORE_WARNING = 10;
    private static final int MAX_LOST_SIGNALS_WARNING_COUNT = 3;
    protected long numberOfMissingSignals;

    public StatLatencyCollector (int windowSize, ByteSequence2LongMap signalsBuffer) {
        this.signalsBuffer = signalsBuffer;
        window = new long [windowSize];
        min = Long.MAX_VALUE;
        max = Long.MIN_VALUE;
    }


    @Override
    public synchronized void recordLatency(byte[] buffer, int offset, int length, long inboundTimestamp, long outboundTimestamp) {
        if (count == 0)
            numberOfMissingSignals = 0; // reset missing signal count, from this moment we will warn users about each missing signal

        final long latency = outboundTimestamp - inboundTimestamp;

        count++;
        window[index] = latency;

        if (min > latency)
            min = latency;
        if (max < latency)
            max = latency;

        sum += latency;

        if (++index == window.length) {
            TimeOfDayFormatter.formatTimeOfDay(System.currentTimeMillis(), timestampBuffer);
            System.out.print(timestampBuffer);
            float loss = 100.0f * numberOfMissingSignals / count;
            System.out.println(" min:" + min + " max:" + max + " avg:" + sum/window.length + " (us) missed:" + numberOfMissingSignals + "(" + loss + "%) used buf:" + signalsBuffer.width());

            index = 0;
            sum = 0;
            min = Long.MAX_VALUE;
            max = Long.MIN_VALUE;
        }
    }

    @Override
    public void missingInboundSignal(byte[] buffer, int offset, int length) {
        numberOfMissingSignals++;
        if (count > 0) {
            if (numberOfMissingSignals <= MAX_LOST_SIGNALS_WARNING_COUNT)
                System.err.println("Can't locate inbound signal " + new String (buffer, offset, length));
            else if (numberOfMissingSignals == MAX_LOST_SIGNALS_WARNING_COUNT)
                System.err.println("Missed more than 3 inbound signals (suppressing further warnings)");
        } else {
            if (numberOfMissingSignals == MAX_INITIAL_SIGNALS_BEFORE_WARNING) {
                System.err.println("Got " + MAX_INITIAL_SIGNALS_BEFORE_WARNING + " outbound signals that doesn't have matching inbound signals. Something is wrong with setup.");
            }
        }
    }

    @Override
    public synchronized void close() {
        if (index > 0)
            System.out.println("min:" + min + " max:" + max + " avg:" + sum/index  + " (us.)");

        System.out.println("Processed " + count + " signals");
    }
}
