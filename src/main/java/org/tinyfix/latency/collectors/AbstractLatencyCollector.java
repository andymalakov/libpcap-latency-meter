package org.tinyfix.latency.collectors;

abstract class AbstractLatencyCollector implements LatencyCollector {
    protected long numberOfMissingSignals;

    @Override
    public void missingInboundSignal(byte[] buffer, int offset, int length) {
        numberOfMissingSignals++;
        if (numberOfMissingSignals <= 3)
            System.err.println("Can't locate inbound signal " + new String (buffer, offset, length));
        else if (numberOfMissingSignals == 4)
            System.err.println("Missed more than 3 inbound signals (suppressing further warnings)");
    }


    @Override
    public void close() {
        if (numberOfMissingSignals > 0)
            System.out.println("Missed inbound ticks for " + numberOfMissingSignals + " orders");
    }
}
