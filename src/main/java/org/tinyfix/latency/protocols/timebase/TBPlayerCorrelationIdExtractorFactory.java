package org.tinyfix.latency.protocols.timebase;

import org.tinyfix.latency.protocols.CorrelationIdExtractor;
import org.tinyfix.latency.protocols.CorrelationIdListener;
import org.tinyfix.latency.protocols.CorrelationIdExtractorFactory;

public class TBPlayerCorrelationIdExtractorFactory<T> implements CorrelationIdExtractorFactory<T> {
    private static final String TB_PLAYBACK_KEY = "tbplayback";

    @Override
    public CorrelationIdExtractorFactory<T> accept(String key) {
        if (key.equals(TB_PLAYBACK_KEY)) {
            return new CorrelationIdExtractorFactory<T>() {
                public CorrelationIdExtractor<T> create(CorrelationIdListener listener) {
                    return new TBPlayerCorrelationIdExtractor<>(listener);
                }
                public String toString() {
                    return "TimeBase Playback";
                }
            };
        }
        return null;
    }
}
