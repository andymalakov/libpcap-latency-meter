package org.tinyfix.latency.collectors;

import org.tinyfix.latency.util.TimeOfDayFormatter;

public class StatLatencyCollector extends AbstractLatencyCollector {
    private final char [] timestampBuffer = new char [TimeOfDayFormatter.FORMAT_LENGTH];
    private final long [] window;
    private long min, max, sum;
    private int index;
    private long count;

    public StatLatencyCollector (int windowSize) {
        window = new long [windowSize];
        min = Long.MAX_VALUE;
        max = Long.MIN_VALUE;
    }

    @Override
    public synchronized void recordLatency(byte[] buffer, int offset, int length, long latency) {
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
            System.out.println(" min:" + min + " max:" + max + " avg:" + sum/window.length + " (us.). Missed: " + numberOfMissingSignals);

            index = 0;
            sum = 0;
            min = Long.MAX_VALUE;
            max = Long.MIN_VALUE;
        }
    }

    @Override
    public synchronized void close() {
        if (index > 0)
            System.out.println("min:" + min + " max:" + max + " avg:" + sum/index  + " (us.)");

        System.out.println("Processed " + count + " signals");
        super.close();
    }
}
