package org.tinyfix.latency.protocols;

import org.jnetpcap.packet.JPacket;

/**
 * Wrapper for CorrelationIdListener that traces in/out signals.
 *
 * This class is slow and alloc-prune, use for debugging only!
 */
public class TracingCorrelationIdListener implements CorrelationIdListener {

    private final String prefix;
    private final CorrelationIdListener delegate;

    public TracingCorrelationIdListener(String prefix, CorrelationIdListener delegate) {
        this.prefix = prefix;
        this.delegate = delegate;
    }

    @Override
    public void onCorrelationId(JPacket packet, byte[] buffer, int offset, int length) {
        System.out.println(prefix + new String (buffer, offset, length));

        delegate.onCorrelationId(packet, buffer, offset, length);
    }
}
