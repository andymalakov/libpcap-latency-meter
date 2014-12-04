package org.tinyfix.latency.protocols.timebase;

import org.tinyfix.latency.protocols.CorrelationIdExtractor;
import org.tinyfix.latency.protocols.CorrelationIdListener;
import org.tinyfix.latency.protocols.CorrelationIdExtractorFactory;

public class TBPlayerCorrelationIdExtractorFactory<T> implements CorrelationIdExtractorFactory<T> {
    private static final String TB_PLAYBACK_KEY = "tbplayback";

    @Override
    public CorrelationIdExtractor<T> create(String protocolKey, CorrelationIdListener listener) {
        if (protocolKey.equals(TB_PLAYBACK_KEY)) {
            return new TBPlayerCorrelationIdExtractor<>(listener);
        }
        return null;
    }
}
