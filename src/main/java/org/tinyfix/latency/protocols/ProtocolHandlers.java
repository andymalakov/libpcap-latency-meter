package org.tinyfix.latency.protocols;

import com.marketfactory.api.MarketFactoryQuoteIdExtractor;

/**
 * Registry of payload protocol handlers
 */
public class ProtocolHandlers {

    private static final String FIX_KEY = "fix:";
    private static final String TIMEBASE_KEY = "timebase";
    private static final String MARKET_FACTORY_KEY = "mf";
    private static final String TB_PLAYBACK_KEY = "tbplayback";

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
        } else
        if (key.equals(TIMEBASE_KEY)) {
            return new ProtocolHandlerFactory<T>() {
                public CorrelationIdExtractor<T> create(CorrelationIdListener listener) {
                    return new TimeBaseQuoteIdExtractor<>(listener);
                }
                public String toString() {
                    return "TimeBase Protocol (HACK)";
                }
            };

        } else
        if (key.equals(MARKET_FACTORY_KEY)) {
            return new ProtocolHandlerFactory<T>() {
                public CorrelationIdExtractor<T> create(CorrelationIdListener listener) {
                    return new MarketFactoryQuoteIdExtractor<>(listener);
                }
                public String toString() {
                    return "MarketFactory Data";
                }
            };
        } else
        if (key.equals(TB_PLAYBACK_KEY)) {
            return new ProtocolHandlerFactory<T>() {
                public CorrelationIdExtractor<T> create(CorrelationIdListener listener) {
                    return new TBPlayerCorrelationIdExtractor<>(listener);
                }
                public String toString() {
                    return "TimeBase Playback";
                }
            };
        }
        throw new IllegalArgumentException("Unsupported protocol handler: \"" + key + '"');
    }
}
