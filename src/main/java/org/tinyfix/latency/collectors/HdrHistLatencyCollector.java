package org.tinyfix.latency.collectors;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogReader;
import org.HdrHistogram.HistogramLogWriter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Uses HdrHistogram to periodically print latency percentiles. Output format is described in {@link HistogramLogReader}.
 */
public class HdrHistLatencyCollector extends SimpleLatencyCollector {

    protected static final long HIGHEST_TRACKABLE_VALUE = Long.parseLong(System.getProperty("histogramHighestTrackableValue", Long.toString(TimeUnit.SECONDS.toMicros(5))));
    protected static final int NUMBER_OF_SIGNIFICANT_VALUE_DIGITS = Integer.getInteger("numberOfSignificantValueDigits", 3);
    private static final int OUTPUT_INTERVAL_MSEC = Integer.getInteger("outputIntervalMsec", 5000);

    protected final HistogramLogWriter histogramLogWriter;

    protected final Histogram histogram = new Histogram(HIGHEST_TRACKABLE_VALUE, NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);

    /** @param outputFileName output file name (in *.hlot format) */
    public HdrHistLatencyCollector (String outputFileName) throws FileNotFoundException {
        PrintStream log = new PrintStream(new FileOutputStream(outputFileName), false);
        histogramLogWriter = new HistogramLogWriter(log);

        // Header
        //histogramLogWriter.outputComment("[Logged with " + getVersionString() + "]");
        histogramLogWriter.outputLogFormatVersion();
        histogramLogWriter.outputStartTime(System.currentTimeMillis());
        histogramLogWriter.setBaseTime(System.currentTimeMillis());
        histogramLogWriter.outputLegend();


        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
                           @Override
                           public void run() {
                               histogramLogWriter.outputIntervalHistogram(histogram);
                           }
                       }, OUTPUT_INTERVAL_MSEC);
    }

    @Override
    protected long getCount() {
        return histogram.getTotalCount();
    }

    @Override
    public void recordLatency(byte[] buffer, int offset, int length, long inboundTimestamp, long outboundTimestamp) {
        histogram.recordValue(outboundTimestamp - inboundTimestamp);
    }
}
