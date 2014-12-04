package com.marketfactory.zerogc.api;

import org.tinyfix.latency.protocols.CorrelationIdExtractor;
import org.tinyfix.latency.protocols.CorrelationIdExtractorFactory;
import org.tinyfix.latency.protocols.CorrelationIdListener;

/** use -p:com.marketfactory.zerogc.api.MarketFactoryQuoteIdExtractorFactory -in:mf */
@SuppressWarnings("unused")
public class MarketFactoryQuoteIdExtractorFactory <T> implements CorrelationIdExtractorFactory<T> {
    private static final String KEY = "mf";

    @Override
    public CorrelationIdExtractor<T> create(String protocolKey, CorrelationIdListener listener) {
        if (protocolKey.startsWith(KEY)) {
            return new MarketFactoryQuoteIdExtractor<>(listener);
        }
        return null;
    }
}
