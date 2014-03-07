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
    private static final long HISTOGRAM_MAX_VALUE = Long.parseLong(System.getProperty("histogramHighestTrackableValue", Long.toString(TimeUnit.MINUTES.toMicros(5))));

    public static void main (String ...args) throws Exception {
        final File inputFile = new File(args[0]);

        String firstLine = null;
        String lastLine = null;

        // A Histogram covering the range from 1 nsec to 1 hour with 5 decimal point resolution:
        final org.HdrHistogram.Histogram histogram = new org.HdrHistogram.Histogram(HISTOGRAM_MAX_VALUE, 3);
        long min = Long.MAX_VALUE, max = 0;
        int signalCount = 0;
        try (LineNumberReader reader = new LineNumberReader(new FileReader(inputFile))) {
            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break; // EOF

                if (firstLine == null)
                    firstLine = line;
                lastLine = line;

                int lastComma = line.lastIndexOf(',');
                long latency = Long.parseLong(line.substring(lastComma+1));
                if (latency > Integer.MAX_VALUE)
                    throw new Exception("Latency value exceeds INT32: " + latency);

                if (min > latency)
                    min = latency;
                if (max < latency)
                    max = latency;

                histogram.recordValue(latency);
                signalCount++;
            }
        }

        if (signalCount > 0) {

            System.out.println("Latency results for " + signalCount + " signals collected from " + cutTimestamp(firstLine) + " ... to " + cutTimestamp(lastLine) + " ()");
            System.out.println("MIN: " + min + " us.");
            System.out.println("MAX: " + max + " us.");
            System.out.println("Histogram: (values below are in milliseconds):.");

            histogram.getHistogramData().outputPercentileDistribution(System.out, 5, 1000.0, false);

            final File histFile = new File(inputFile.getAbsolutePath() + "-histogram.csv");
            try (PrintStream ps = new PrintStream(histFile)) {
                histogram.getHistogramData().outputPercentileDistribution(ps, 5, 1000.0, true);
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
