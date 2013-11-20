package org.tinyfix.latency.protocols;

public interface ProtocolHandlerFactory<T> {
    CorrelationIdExtractor<T> create(CorrelationIdListener listener);
}
