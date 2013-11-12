package org.tinyfix.latency.util;

/**
 * Self-purging map byte[] => long.
 */
public interface ByteSequence2LongMap {

    public static final long NOT_FOUND = -1L;

    /**
     * Stores timestamp for given key
     */
    void put (byte [] keyBuffer, int offset, int length, long timestamp);

    /**
     * Gets value stored for given key. Implementation may remove entry after this call.
     * @return Timestamp for given key, or -1L of key not found */
    long get(byte[] keyBuffer, int offset, int length);

}
