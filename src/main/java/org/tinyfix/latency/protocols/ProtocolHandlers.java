package org.tinyfix.latency.protocols;

import org.tinyfix.latency.protocols.fix.FixMessageTagExtractorFactory;
import org.tinyfix.latency.protocols.timebase.TBPlayerCorrelationIdExtractorFactory;
import org.tinyfix.latency.protocols.timebase.TimeBaseQuoteIdExtractorFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry of payload protocol handlers
 */
public class ProtocolHandlers<T> {


    private final List<CorrelationIdExtractorFactory<T>> protocolHandlers = new ArrayList<>();

    public ProtocolHandlers() {
        // list of standard protocols
        addProtocolHandler(new FixMessageTagExtractorFactory<T>());
        addProtocolHandler(new TBPlayerCorrelationIdExtractorFactory<T>());
        addProtocolHandler(new TimeBaseQuoteIdExtractorFactory<T>());
    }

    @SuppressWarnings("unchecked")
    public void addProtocolHandler (String protocolClassName) {
        try {
            Object metaFactory = Class.forName(protocolClassName).newInstance();
            if (!(metaFactory instanceof CorrelationIdExtractorFactory))
                throw new IllegalArgumentException("Class " + protocolClassName + " does not implement CorrelationIdExtractorFactory interface");
            addProtocolHandler((CorrelationIdExtractorFactory<T>) metaFactory);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid custom protocol: " + protocolClassName, e);
        }
    }

    public void addProtocolHandler (CorrelationIdExtractorFactory<T> protocolClass) {
        protocolHandlers.add(protocolClass);
    }

    public CorrelationIdExtractor<T> create(String handlerKey, CorrelationIdListener idListener) {
        for (CorrelationIdExtractorFactory<T> factory : protocolHandlers) {
            CorrelationIdExtractor<T> result = factory.create(handlerKey, idListener);
            if (result != null)
                return result;

        }
        throw new IllegalArgumentException("Unsupported protocol handler: \"" + handlerKey + '"');
    }
}
