package org.tinyfix.latency.util;

import java.util.HashMap;
import java.util.Map;

public class HashMapByteSequence2LongMap implements ByteSequence2LongMap {
    private final Map<String,Long> map = new HashMap<>();

    @Override
    public long get(byte[] buffer, int offset, int length) {
        Long result = map.remove(new String(buffer, offset, length));
        return (result != null) ? result.longValue() : NOT_FOUND;
    }

    @Override
    public void put(byte[] buffer, int offset, int length, long timestamp) {
        map.put(new String(buffer, offset, length), timestamp);
    }
}
