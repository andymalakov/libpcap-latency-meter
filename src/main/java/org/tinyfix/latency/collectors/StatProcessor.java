package org.tinyfix.latency.collectors;

import java.io.*;
import java.util.Arrays;

public class StatProcessor {
    public static void main (String ...args) throws Exception {
        final String inputFile = args[0];
        final int numberOfPoints = countNumberOfLines(inputFile) + 1;
        final int [] sortedLatencies = new int [numberOfPoints];

        String firstLine = null;
        String lastLine = null;


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
                sortedLatencies[signalCount] = (int)latency;

                signalCount++;
            }
        }

        if (signalCount > 0) {

            System.out.println("Sorting " + signalCount + " results (from " + cutTimestamp(firstLine) + " ... to " + cutTimestamp(lastLine) + ")");
            Arrays.sort(sortedLatencies, 0, signalCount);
            System.out.println("MIN: " + sortedLatencies[0]);
            System.out.println("MAX: " + sortedLatencies[signalCount-1]);
            System.out.println("MEDIAN: " + sortedLatencies[signalCount/2]);

            System.out.println("99.000%: " + sortedLatencies[ (int)   (99L*signalCount/100)]);
            System.out.println("99.900%: " + sortedLatencies[ (int)  (999L*signalCount/1000)]);
            System.out.println("99.990%: " + sortedLatencies[ (int) (9999L*signalCount/10000)]);
            System.out.println("99.999%: " + sortedLatencies[ (int)(99999L*signalCount/100000)]);
            System.out.println("99.9999%: " + sortedLatencies[ (int)(999999L*signalCount/1000000)]);
            System.out.println("99.99999%:" + sortedLatencies[ (int)(9999999L*signalCount/10000000)]);
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

    private static int countNumberOfLines(String filename) throws IOException {
        int count = 0;
        boolean isEmpty = true;

        try (InputStream is = new BufferedInputStream(new FileInputStream(filename))) {
            byte[] c = new byte[1024];
            int readChars;
            while ((readChars = is.read(c)) != -1) {
                isEmpty = false;
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
        }
        return (count == 0 && !isEmpty) ? 1 : count;
    }
}
