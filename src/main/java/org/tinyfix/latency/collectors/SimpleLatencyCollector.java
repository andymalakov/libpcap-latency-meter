package org.tinyfix.latency.collectors;

abstract class SimpleLatencyCollector implements LatencyCollector {
    private static final int MAX_INITIAL_SIGNALS_BEFORE_WARNING = 10;
    private static final int MAX_LOST_SIGNALS_WARNING_COUNT = 3;
    protected long numberOfMissingSignals;

    @Override
    public void missingInboundSignal(byte[] buffer, int offset, int length) {
        numberOfMissingSignals++;
        if (getCount() > 0) {
            if (numberOfMissingSignals <= MAX_LOST_SIGNALS_WARNING_COUNT)
                System.err.println("Can't locate inbound signal " + new String (buffer, offset, length));
            else if (numberOfMissingSignals == MAX_LOST_SIGNALS_WARNING_COUNT)
                System.err.println("Missed more than 3 inbound signals (suppressing further warnings)");
        } else {
            if (numberOfMissingSignals == MAX_INITIAL_SIGNALS_BEFORE_WARNING) {
                System.err.println("Got " + MAX_INITIAL_SIGNALS_BEFORE_WARNING + " outbound signals that doesn't have matching inbound signals. Something is wrong with setup.");
            }
        }

    }

    protected abstract long getCount();

    @Override
    public void close() {
        // by default does nothing
    }
}
