package org.tinyfix.latency.common;

public class CaptureSettings {

    /** maximum size of correlation key (in bytes) */
    public static final int MAX_CORRELATION_ID_LENGTH = getIntProperty("maxCorrelationIdLength", 20);

    /** Timestamp of inbound messages are stored in fixed size ring buffer */
    public static final int RING_BUFFER_CAPACITY = getIntProperty("ringBufferCapacity", 16*1024);

    /** Maximum Amount of data captured for each packet (this parameter has effect only in live mode and only with capture filter) */
    public static final int PACKET_SNAP_LENGTH = getIntProperty("packetSnapLength", 64 * 1024);

    /** Timestamp of inbound messages are stored in fixed size ring buffer */
    public static final boolean DUMP_TIMESTAMPS = getBoolProperty("dumpTimestamps", false);

    /** Interface .openLive() initialization timeout in milliseconds */
    public static final int OPEN_LIVE_TIMEOUT_MILLIS = getIntProperty("packetSnapLen", 10 * 1000);


    public static final boolean OPTIMIZE_FILTER = getBoolProperty("optimizeFilter", false);

    /** Network mask that is used to compile capture filter. Defined as hex string (e.g. "FFFFFF00" = 255.255.255.0) */
    public static final String FILTER_NETWORK_MASK_HEX = System.getProperty("filterNetMask", "FFFFFF00");


    private static final int getIntProperty (String propertyKey, int defaultValue) {
        String property = System.getProperty(propertyKey);
        if (property == null)
            return defaultValue;
        return Integer.parseInt(property);
    }

    private static final boolean getBoolProperty (String propertyKey, boolean defaultValue) {
        String property = System.getProperty(propertyKey);
        if (property == null)
            return defaultValue;
        return Boolean.parseBoolean(property);
    }

}
