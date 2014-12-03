package org.tinyfix.latency.protocols.timebase;

import org.tinyfix.latency.protocols.CorrelationIdExtractor;
import org.tinyfix.latency.protocols.CorrelationIdListener;
import org.tinyfix.latency.protocols.CorrelationIdExtractorFactory;
import org.tinyfix.latency.protocols.CorrelationIdExtractorFactory;

public class TimeBaseQuoteIdExtractorFactory<T> implements CorrelationIdExtractorFactory<T> {
    private static final String TIMEBASE_KEY = "timebase";

    @Override
    public CorrelationIdExtractorFactory<T> accept(String key) {
        if (key.equals(TIMEBASE_KEY)) {
            return new CorrelationIdExtractorFactory<T>() {
                public CorrelationIdExtractor<T> create(CorrelationIdListener listener) {
                    return new TimeBaseQuoteIdExtractor<>(listener);
                }
                public String toString() {
                    return "TimeBase Protocol (HACK)";
                }
            };

        }
        return null;
    }
}
