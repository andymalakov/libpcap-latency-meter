package org.tinyfix.latency.protocols;

/**
 * Registry of payload protocol handlers
 */
public class ProtocolHandlers {

    private static final String FIX_KEY = "fix:";
    private static final String TIMEBASE_KEY = "timebase";

    public static <T> ProtocolHandlerFactory<T> getProtocolHandler(final String key) {
        if (key.startsWith(FIX_KEY)) {
            return new ProtocolHandlerFactory<T>() {
                public CorrelationIdExtractor<T> create(CorrelationIdListener listener) {
                    int fixTagNumber = Integer.parseInt(key.substring(FIX_KEY.length()));
                    return new FixMessageTagExtractor<>(fixTagNumber, listener);
                }
                public String toString() {
                    return "FIX Protocol (tag " + key.substring(FIX_KEY.length()) + ')';
                }
            };
        }
        if (key.equals(TIMEBASE_KEY)) {
            return new ProtocolHandlerFactory<T>() {
                public CorrelationIdExtractor<T> create(CorrelationIdListener listener) {
                    return new TimeBaseQuoteIdExtractor<>(listener);
                }
                public String toString() {
                    return "TimeBase Protocol (HACK)";
                }
            };

        }
        throw new IllegalArgumentException("Unsupported protocol handler: \"" + key + '"');
    }
}
