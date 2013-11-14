package org.tinyfix.latency.util;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class TimeOfDayFormatter {

    private final static long SECONDS_IN_DAY = TimeUnit.DAYS.toSeconds(1);

    private static final String MIDNIGHT = "00:00:00";
    private static final byte [] MIDNIGHT_BYTES = MIDNIGHT.getBytes();
    private static final char [] MIDNIGHT_CHARS = MIDNIGHT.toCharArray();
    public static final int FORMAT_LENGTH = MIDNIGHT_BYTES.length;

    /** Offset of local time zone from GMT (in milliseconds) */
    private final static long MILLIS_GMT_OFFSET;
    static {
        Calendar c = Calendar.getInstance();
        MILLIS_GMT_OFFSET = c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET);
    }

    private TimeOfDayFormatter () {}


    /**
     * Fast and thread-safe method for printing *local* time of day from java "epoch" time.
     *
     * @param timestamp Java time from System.currentTimeMillis().
     * @param outputBuffer buffer for formatted time (Output will look like "01:23:45"). Must accomodate 8 bytes of formatted value.
     */
    public static void formatTimeOfDay (final long timestamp, final byte [] outputBuffer) {
        final long seconds = (timestamp + MILLIS_GMT_OFFSET) / 1000;
        System.arraycopy(MIDNIGHT_BYTES, 0, outputBuffer, 0, FORMAT_LENGTH);

        if (seconds != 0) {

            int secondsInDay = (int) (seconds % SECONDS_IN_DAY);

            int     s = secondsInDay % 60;
            int     m = (secondsInDay / 60) % 60;
            int     h = secondsInDay / 3600;

            // H low
            int foo = h % 10;
            if (foo > 0)
                outputBuffer [1] += foo;

            // H high
            foo = h / 10;
            if (foo > 0)
                outputBuffer [0] += foo;

            // M low
            foo = m % 10;
            if (foo > 0)
                outputBuffer [4] += foo;

            // M high
            foo = m / 10;
            if (foo > 0)
                outputBuffer [3] += foo;

            // S low
            foo = s % 10;
            if (foo > 0)
                outputBuffer [7] += foo;

            // S high
            foo = s  / 10;
            if (foo > 0)
                outputBuffer [6] += foo;
        }
    }

    /**
     * Fast and thread-safe method for printing *local* time of day from java "epoch" time.
     *
     * @param timestamp Java time from System.currentTimeMillis().
     * @param outputBuffer buffer for formatted time (Output will look like "01:23:45"). Must accomodate 8 bytes of formatted value.
     */
    public static void formatTimeOfDay (final long timestamp, final char [] outputBuffer) {
        final long seconds = (timestamp + MILLIS_GMT_OFFSET) / 1000;
        System.arraycopy(MIDNIGHT_CHARS, 0, outputBuffer, 0, FORMAT_LENGTH);

        if (seconds != 0) {

            int secondsInDay = (int) (seconds % SECONDS_IN_DAY);

            int     s = secondsInDay % 60;
            int     m = (secondsInDay / 60) % 60;
            int     h = secondsInDay / 3600;

            // H low
            int foo = h % 10;
            if (foo > 0)
                outputBuffer [1] += foo;

            // H high
            foo = h / 10;
            if (foo > 0)
                outputBuffer [0] += foo;

            // M low
            foo = m % 10;
            if (foo > 0)
                outputBuffer [4] += foo;

            // M high
            foo = m / 10;
            if (foo > 0)
                outputBuffer [3] += foo;

            // S low
            foo = s % 10;
            if (foo > 0)
                outputBuffer [7] += foo;

            // S high
            foo = s  / 10;
            if (foo > 0)
                outputBuffer [6] += foo;
        }
    }
}
