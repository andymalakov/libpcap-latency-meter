package org.tinyfix.latency.collectors;

import org.HdrHistogram.Histogram;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.PrintStream;

public class StatProcessor {
    private static final int PERCENTILE_TICKS_PER_HALF_DISTANCE = Integer.getInteger("percentileTicksPerHalfDistance", 10);
    private static final double OUTPUT_VALUE_UNIT_SCALING_RATIO = Integer.getInteger("outputValueUnitScalingRatio", 1); // was 1000 (milliseconds)
    private static final boolean USE_CSV_FORMAT = Boolean.getBoolean("useCsvFormat");

    public static void main (String ...args) throws Exception {
        final File inputFile = new File(args[0]);

        String firstLine = null;
        String lastLine = null;

        // A Histogram covering the range from 1 nsec to 1 hour with 5 decimal point resolution:

        final Histogram histogram = new Histogram(HdrHistLatencyCollector.HIGHEST_TRACKABLE_VALUE, HdrHistLatencyCollector.NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);

        int signalCount = 0;
        try (LineNumberReader reader = new LineNumberReader(new FileReader(inputFile))) {
            reader.readLine(); // skip header
            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break; // EOF

                if (firstLine == null)
                    firstLine = line;
                lastLine = line;

                int lastComma = line.lastIndexOf(',');
                long latency = Long.parseLong(line.substring(lastComma+1).trim());
                if (latency > Integer.MAX_VALUE)
                    throw new Exception("Latency value exceeds INT32: " + latency);

                histogram.recordValue(latency);
                signalCount++;
            }
        }

        if (signalCount > 0) {
            System.out.println("Latency results for " + signalCount + " signals collected from " + cutTimestamp(firstLine) + " ... to " + cutTimestamp(lastLine) + " ()");
            System.out.println("MIN    : " + histogram.getValueAtPercentile(0.0)+ " us.");
            System.out.println("50.000%: " + histogram.getValueAtPercentile(50)+ " us.");
            System.out.println("90.000%: " + histogram.getValueAtPercentile(90)+ " us.");
            System.out.println("99.000%: " + histogram.getValueAtPercentile(99)+ " us.");
            System.out.println("99.900%: " + histogram.getValueAtPercentile(99.9)+ " us.");
            System.out.println("99.990%: " + histogram.getValueAtPercentile(99.99)+ " us.");
            System.out.println("99.999%: " + histogram.getValueAtPercentile(99.999)+ " us.");
            System.out.println("MAX    : " + histogram.getValueAtPercentile(100)+ " us.");
            System.out.println("Histogram: (values below are in microseconds):");

            final File histFile = new File(inputFile.getAbsolutePath() + "-histogram.csv");
            try (PrintStream ps = new PrintStream(histFile)) {
                histogram.outputPercentileDistribution(ps, PERCENTILE_TICKS_PER_HALF_DISTANCE, OUTPUT_VALUE_UNIT_SCALING_RATIO, USE_CSV_FORMAT);
            }
            System.out.println("Saved histogram into " + histFile.getAbsolutePath() + " ...");
        }
    }

    private static String cutTimestamp(String line) {
        if (line != null) {
            int commaIndex = line.indexOf(',');
            if (commaIndex > 0)
                return line.substring(0, commaIndex);
        }
        return  "?";
    }
}
