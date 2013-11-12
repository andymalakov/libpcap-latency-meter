package org.tinyfix.latency.collectors;

public class ChainedLatencyCollector implements LatencyCollector {

    private final LatencyCollector [] chain;

    public ChainedLatencyCollector(LatencyCollector... chain) {
        this.chain = chain;
    }

    @Override
    public void recordLatency(byte[] buffer, int offset, int length, long latency) {
        for (int i = 0; i < chain.length; i++) {
            chain[i].recordLatency(buffer, offset, length, latency);
        }
    }

    @Override
    public void missingInboundSignal(byte[] buffer, int offset, int length) {
        for (int i = 0; i < chain.length; i++) {
            chain[i].missingInboundSignal(buffer, offset, length);
        }
    }

    @Override
    public void close() {
        for (int i = 0; i < chain.length; i++) {
            chain[i].close();
        }
    }
}
