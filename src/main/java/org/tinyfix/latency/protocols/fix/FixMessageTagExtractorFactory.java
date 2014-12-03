package org.tinyfix.latency.protocols.fix;

import org.tinyfix.latency.protocols.CorrelationIdExtractor;
import org.tinyfix.latency.protocols.CorrelationIdListener;
import org.tinyfix.latency.protocols.CorrelationIdExtractorFactory;

public class FixMessageTagExtractorFactory<T> implements CorrelationIdExtractorFactory<T> {
    private static final String FIX_KEY = "fix:";

    @Override
    public CorrelationIdExtractor<T> create(String protocolKey, CorrelationIdListener listener) {
        if (protocolKey.startsWith(FIX_KEY)) {
            int fixTagNumber = Integer.parseInt(protocolKey.substring(FIX_KEY.length()));
            return new FixMessageTagExtractor<>(fixTagNumber, listener);
        }
        return null;
    }
}
