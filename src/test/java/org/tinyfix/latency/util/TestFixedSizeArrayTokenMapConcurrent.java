package org.tinyfix.latency.util;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TestFixedSizeArrayTokenMapConcurrent {

    private static final int maxCapacity = 1 << 16;
    private static final int maxKeyLength = 32;

    @Test
    public void testProducerConsumer() throws InterruptedException {
        BlockingQueue queue = new LinkedBlockingQueue();
        FixedSizeArrayTokenMap buffer = new FixedSizeArrayTokenMap(maxCapacity, maxKeyLength);
        ProducerThread producer = new ProducerThread (buffer, 5, queue);
        ConsumerThread consumer = new ConsumerThread (buffer, 5, 7, queue);

        producer.start();
        consumer.start();

        Thread.sleep (15000);

        producer.close();
        consumer.close();
    }

    private static abstract class BufferTestThread extends Thread {
        protected final FixedSizeArrayTokenMap buffer;
        protected final BlockingQueue queue;
        private volatile boolean active = true;


        private BufferTestThread(String name, FixedSizeArrayTokenMap buffer, BlockingQueue queue) {
            this.queue = queue;
            setName(name);
            this.buffer = buffer;
        }

        @Override
        public void run() {
            try {
                long sequence = 1;
                while (active) {
                    run (sequence, Long.toString(sequence).getBytes());
                    sequence++;
                }
            } catch (Throwable e) {
                if (e instanceof InterruptedException) {
                    System.err.println (getName() + " was interrupted");
                } else {
                    System.err.println ("Error in " + getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        protected abstract void run(long sequence, byte[] key) throws InterruptedException;

        void close() throws InterruptedException {
            active = false;
            this.interrupt();
            this.join();
        }
    }

    private static class ProducerThread extends BufferTestThread {
        private final int sleepInterval;
        private final Random rnd = new Random(System.currentTimeMillis());

        private ProducerThread(FixedSizeArrayTokenMap buffer, int sleepInterval, BlockingQueue queue) {
            super("Producer", buffer, queue);
            this.sleepInterval = sleepInterval;
        }

        @Override
        protected void run(long sequence, byte[] key) throws InterruptedException {
            buffer.put(key, 0, key.length, sequence);
            queue.put (key); // put anything
            Thread.sleep(sleepInterval/2 + rnd.nextInt(sleepInterval));
        }
    }

    private static class ConsumerThread extends BufferTestThread {
        private final int consumeStep;
        private final int sleepInterval;
        private final Random rnd = new Random(System.currentTimeMillis());

        private ConsumerThread(FixedSizeArrayTokenMap buffer, int sleepInterval, int consumeStep, BlockingQueue queue) {
            super("Consumer", buffer, queue);
            this.consumeStep = consumeStep;
            this.sleepInterval = sleepInterval;
        }

        @Override
        protected void run(long sequence, byte[] key) throws InterruptedException {
            queue.take();
            if (sequence % consumeStep == 0) {
                long value = buffer.get(key, 0, key.length);
                if (value == FixedSizeArrayTokenMap.NOT_FOUND)
                    Assert.fail ("Lost signal#" + new String(key));
                if (value != sequence)
                    Assert.fail("Sequence for key " + new String(key) + " is incorrect. Expeted: " + sequence + " got: " + value);

                System.out.println("Got " + value + " width: " + buffer.width());
            }

            Thread.sleep(rnd.nextInt(2*sleepInterval));
        }
    }

}
