package org.tinyfix.latency.collectors;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

/**
 * @author Andy
 *         Date: 3/5/14
 */
public class Histogram {
    private static final double outputValueUnitScalingRatio = Integer.getInteger("outputValueUnitScalingRatio", 1); // was 1000 (milliseconds)
    private static final int numberOfSignificantValueDigits = Integer.getInteger("numberOfSignificantValueDigits", 3); // was: 3 (up to microsecond)
    private static final int percentileTicksPerHalfDistance = Integer.getInteger("percentileTicksPerHalfDistance", 10);

    private static final long HISTOGRAM_MAX_VALUE = Long.parseLong(System.getProperty("histogramHighestTrackableValue", Long.toString(TimeUnit.SECONDS.toMicros(5))));

    public static void main (String ...args) throws Exception {
        final File inputFile = new File(args[0]);

        String firstLine = null;
        String lastLine = null;

        // A Histogram covering the range from 1 nsec to 1 hour with 5 decimal point resolution:

        final org.HdrHistogram.Histogram histogram = new org.HdrHistogram.Histogram(HISTOGRAM_MAX_VALUE, numberOfSignificantValueDigits);

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
            System.out.println("MIN:   "   + histogram.getHistogramData().getValueAtPercentile(0.0)+ " us.");
            System.out.println("50.000%:"  + histogram.getHistogramData().getValueAtPercentile(50)+ " us.");
            System.out.println("90.000%: " + histogram.getHistogramData().getValueAtPercentile(90)+ " us.");
            System.out.println("99.000%: " + histogram.getHistogramData().getValueAtPercentile(99)+ " us.");
            System.out.println("99.900%: " + histogram.getHistogramData().getValueAtPercentile(99.9)+ " us.");
            System.out.println("99.990%: " + histogram.getHistogramData().getValueAtPercentile(99.99)+ " us.");
            System.out.println("99.999%: " + histogram.getHistogramData().getValueAtPercentile(99.999)+ " us.");
            System.out.println("MAX    : " + histogram.getHistogramData().getValueAtPercentile(100)+ " us.");
            System.out.println("Histogram: (values below are in microseconds):");

            final File histFile = new File(inputFile.getAbsolutePath() + "-histogram.csv");
            try (PrintStream ps = new PrintStream(histFile)) {
                histogram.getHistogramData().outputPercentileDistribution(ps, percentileTicksPerHalfDistance, outputValueUnitScalingRatio, true);
            }
            System.out.println("Saved histogram into " + histFile.getAbsolutePath() + " ...");

        }

//        long interval = 0;
//        if (interval > 0) {
//            System.out.println("Results corrected for coordinated omission (assuming " + interval + " interval between signals)");
//
//            histogram.copyCorrectedForCoordinatedOmission(interval).getHistogramData().outputPercentileDistribution(System.out, percentileTicksPerHalfDistance, outputValueUnitScalingRatio, false);
//        }


    }

    private static String cutTimestamp(String line) {
        if (line != null) {
            int commaIndex = line.indexOf(',');
            if (commaIndex > 0)
                return line.substring(0, commaIndex);
        }
        return  "?";
    }
//
//    private static int countNumberOfLines(String filename) throws IOException {
//        int count = 0;
//        boolean isEmpty = true;
//
//        try (InputStream is = new BufferedInputStream(new FileInputStream(filename))) {
//            byte[] c = new byte[1024];
//            int readChars;
//            while ((readChars = is.read(c)) != -1) {
//                isEmpty = false;
//                for (int i = 0; i < readChars; ++i) {
//                    if (c[i] == '\n') {
//                        ++count;
//                    }
//                }
//            }
//        }
//        return (count == 0 && !isEmpty) ? 1 : count;
//    }

}
