package org.tinyfix.latency.util;

import java.util.List;

public class KeyValueRingBuffer implements ByteSequence2LongMap {

    private static class CircularBuffer {
        private final BufferEntry[] entries;

        private long head = -1; // points to last produced entry
        private long tail = -1; // points to last consumed entry
        private final int bufferSize, indexMask;

        public CircularBuffer(int bufferSize, int maxKeyLength) {
            if (bufferSize < 0 || Integer.bitCount(bufferSize) != 1){
                throw new IllegalArgumentException("capacity must be a power of 2");
            }

            this.indexMask = bufferSize - 1;
            this.bufferSize = bufferSize;

            // preallocate space for correlation IDs
            this.entries = new BufferEntry [bufferSize];
            for (int i=0; i < bufferSize; i++) {
                entries[i] = new BufferEntry(maxKeyLength);
            }
        }

        private BufferEntry index(long sequence) {
            return entries[((int)sequence) & indexMask];
        }

        public synchronized BufferEntry put() {
            head++;
            assert head > tail;
            return index(head);
        }

        public synchronized BufferEntry get() {
            long delta = head - tail;
            if (delta == 0)
                return null; // empty

            // check overflow
            if (delta > bufferSize)
                tail = head - bufferSize + 1;
            else
                tail++;

            assert head >= tail;
            return index(tail);
        }

        synchronized void appendTo(List<BufferEntry> result) {
            long t = tail + 1;
            long span = head - bufferSize;
            if (t < span)
                t = span + 1;

            while (t <= head) {
                result.add (index(t++));
            }
        }

        synchronized long width() {
            return head - tail;
        }

        @Override
        public synchronized String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("H:");
            sb.append(head);
            sb.append(" T:");
            sb.append(tail);
            sb.append(" {");
            for (int i = 0; i < bufferSize; i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(entries[i]);
            }
            sb.append('}');
            return sb.toString();
        }
    }

    static class BufferEntry {
        private final byte [] keyBuffer;
        private int keyBufferLength;
        private long value;

        BufferEntry (int maxKeyLength) {
            keyBuffer = new byte[maxKeyLength];
        }

        public void set(byte[] keyBuffer, int offset, int length, long value) {
            System.arraycopy(keyBuffer, offset, this.keyBuffer, 0, length);
            keyBufferLength = length;
            this.value = value;
        }

        public boolean equals(byte[] keyBuffer, int offset, int length) {
            if (keyBufferLength == length) {
                for (int i = 0; i < length; i++) {
                    if (this.keyBuffer[i] != keyBuffer[i+offset])
                        return false;
                }
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return new String (keyBuffer, 0, keyBufferLength) + '=' + value;
        }
    }

    private final int maxKeyLength;
    private final CircularBuffer buffer;

    public KeyValueRingBuffer(int maxCapacity, int maxKeyLength) {
        this.buffer = new CircularBuffer(maxCapacity, maxKeyLength);
        this.maxKeyLength = maxKeyLength;
    }

    /**
     * Stores timestamp for given key
     */
    @Override
    public void put (byte [] keyBuffer, int offset, int length, long timestamp) {
        if (length > maxKeyLength)
            throw new IllegalArgumentException();

        BufferEntry entry = buffer.put();
        entry.set(keyBuffer, offset, length, timestamp);
    }

    /** @return Timestamp for given key, or -1L of key not found */
    @Override
    public long get(byte[] keyBuffer, int offset, int length) {

        for (BufferEntry entry = buffer.get(); entry != null; entry = buffer.get()) {
            if (entry.equals(keyBuffer, offset, length))
                return entry.value;

        }
        return  NOT_FOUND;
    }

    /**
     * Used by logging
     * @return in case of overruns width may exceed buffer size
     */
    @Override
    public long width() {
        return buffer.width();
    }

    // Unit tests
    void snapshot(List<BufferEntry> content) {
        content.clear();
        buffer.appendTo(content);
    }

    BufferEntry get() {
        return buffer.get();
    }

    @Override
    public String toString () {
        return dump();
    }

    // Unit tests
    String dump() {
        return buffer.toString();
    }

}
