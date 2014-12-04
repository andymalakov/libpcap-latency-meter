package org.tinyfix.latency.protocols.timebase;

import org.tinyfix.latency.protocols.CorrelationIdExtractor;
import org.tinyfix.latency.protocols.CorrelationIdExtractorFactory;
import org.tinyfix.latency.protocols.CorrelationIdListener;

public class TimeBaseQuoteIdExtractorFactory<T> implements CorrelationIdExtractorFactory<T> {
    private static final String TIMEBASE_KEY = "timebase";

    @Override
    public CorrelationIdExtractor<T> create(String protocolKey, CorrelationIdListener listener) {
        if (protocolKey.equals(TIMEBASE_KEY)) {
            return new TimeBaseQuoteIdExtractor<>(listener);

        }
        return null;
    }
}
