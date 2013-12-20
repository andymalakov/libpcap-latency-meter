package org.tinyfix.latency.collectors;

import org.tinyfix.latency.util.TimeOfDayFormatter;

import java.io.*;
import java.util.Arrays;

public class StatProcessor {
    public static void main (String ...args) throws Exception {
        final String inputFile = args[0];
        final int numberOfPoints = Integer.parseInt(args[1]);
        final int [] sortedLatencies = (numberOfPoints > 0) ? new int [numberOfPoints] : null;

        int signalCount = 0;
        try (LineNumberReader reader = new LineNumberReader(new FileReader(inputFile))) {
            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break; // EOF

                int lastComma = line.lastIndexOf(',');
                long latency = Long.parseLong(line.substring(lastComma+1));


                if (sortedLatencies != null) {
                    if (latency > Integer.MAX_VALUE)
                        throw new Exception("Latency value exceeds INT32: " + latency);
                    sortedLatencies[signalCount] = (int)latency;
                }

                signalCount++;
            }
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
            System.out.println("99.9999%: " + sortedLatencies[ (int)(999999L*signalCount/1000000)]);
        }


    }
}
