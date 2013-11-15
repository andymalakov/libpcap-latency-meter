package org.tinyfix.latency.util;

import java.util.HashMap;
import java.util.Map;

/** Currently unused dummy implementation of ByteSequence2LongMap */
@SuppressWarnings("unused")
public class HashMapByteSequence2LongMap implements ByteSequence2LongMap {

    //TODO: need mechanism of expunging unclaimed entries after some timeout or keeping constant max size
    private final Map<String,Long> map;

    public HashMapByteSequence2LongMap(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

    @Override
    public synchronized long get(byte[] buffer, int offset, int length) {
        Long result = map.remove(new String(buffer, offset, length));
        return (result != null) ? result.longValue() : NOT_FOUND;
    }

    @Override
    public synchronized void put(byte[] buffer, int offset, int length, long timestamp) {
        map.put(new String(buffer, offset, length), timestamp);
    }

    @Override
    public synchronized long width() {
        return map.size();
    }
}
