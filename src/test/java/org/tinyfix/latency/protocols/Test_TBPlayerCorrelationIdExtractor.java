package org.tinyfix.latency.protocols;


import org.jnetpcap.packet.JPacket;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

public class Test_TBPlayerCorrelationIdExtractor {

    private final StringBuilder lastCorrelationId = new StringBuilder();

    private final CorrelationIdListener correlationIdListener = new CorrelationIdListener () {

        @Override
        public void onCorrelationId(JPacket packet, byte[] buffer, int offset, int length) {
            lastCorrelationId.setLength(0);
            for (int i=0; i < length; i++)
                lastCorrelationId.append((char)buffer[offset+i]);
        }
    };
    private final TBPlayerCorrelationIdExtractor extractor = new TBPlayerCorrelationIdExtractor (correlationIdListener);


    @Test
    public void testSimple() throws IOException {
        assertEncoding(123456);
    }

    @Test
    public void testRandom() throws IOException {
        assertEncoding(576460752303423488L);
        assertEncoding(0xff1f000000000000L);
    }

    @Test
    public void testAddingBit () throws IOException {
        byte [] buf = new byte [8];

        for (int i = 0; i < buf.length; i++) {
            int mask = 1;
            for (int b=0; b<8; b++) {
                buf[i] |= (byte) mask;

                long expected = TestHelper.getBigEndianLong(buf);
                assertEncoding(expected);

                mask = mask << 1;
            }
        }
    }

    @Test
    public void testWalkingBit () throws IOException {
        byte [] buf = new byte [8];

        for (int i = 0; i < buf.length; i++) {
            int mask = 1;
            for (int b=0; b<8; b++) {
                buf[i] = (byte) mask;

                long expected = TestHelper.getBigEndianLong(buf);
                assertEncoding(expected);

                mask = mask << 1;
                buf[i] = 0;
            }
        }
    }

    @Test
    public void testBufferProcessing1() {
        byte [] packet = {(byte)0x89, (byte)0x3A, (byte)0x34, (byte)0xE2, (byte)0x2F, (byte)0x4F, (byte)0x9A, (byte)0x66, (byte)0x72, (byte)0xC0, (byte)0xC0, (byte)0xDE, (byte)0x7, (byte)0xFF, (byte)0xF, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x4D, (byte)0x3A, (byte)0xDC, (byte)0x28, (byte)0xD5, (byte)0x1B, (byte)0x4, (byte)0x7B, (byte)0x4D, (byte)0xFE};
        assertEncoding(packet, 0xFF0F000000000000L);
    }

    /** Magic number is prefixed with C0 */
    @Test
    public void testC0C0D0Buffer() {
        ///                        1          2          3          4          5          6          7         8         9          10         11          ( MAGIC(3)+LONG(8)=11 }
        byte [] packet0 = { (byte)0xC0, (byte)0xDE, (byte)0x7, (byte)0xFF, (byte)0xF, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0 };
        assertEncoding(packet0, 0xFF0F000000000000L);

        byte [] packet1 = { (byte)0xC0, (byte)0xC0, (byte)0xDE, (byte)0x7, (byte)0xFF, (byte)0xF, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0 };
        assertEncoding(packet1, 0xFF0F000000000000L);

        byte [] packet2 = { (byte)0xC0, (byte)0xDE, (byte)0xC0, (byte)0xDE, (byte)0x7, (byte)0xFF, (byte)0xF, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0 };
        assertEncoding(packet2, 0xFF0F000000000000L);

        byte [] packet3 = { (byte)0xC0, (byte)0xC0, (byte)0xDE, (byte)0xC0, (byte)0xDE, (byte)0x7, (byte)0xFF, (byte)0xF, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0 };
        assertEncoding(packet3, 0xFF0F000000000000L);

    }

    private void assertEncoding (byte [] packetBytes, long expectedValue) {
        JTestPacket packet = new JTestPacket (packetBytes);
        extractor.parse(packet, 0, packet.getLength(), null);
        if (lastCorrelationId.length() == 0)
            Assert.fail ("Failed to find correlation ID");
        long actualCorrelationId = Long.parseLong(lastCorrelationId.toString());
        String diagnostic = "Got " + Long.toHexString(actualCorrelationId);
        Assert.assertEquals(diagnostic, expectedValue, actualCorrelationId);
    }

    private void assertEncoding(long expectedValue) throws IOException {
        JTestPacket packet = makePacket (expectedValue, 10, 10);
        extractor.parse(packet, 0, packet.getLength(), null);
        if (lastCorrelationId.length() == 0)
            Assert.fail ("Failed to find correlation ID");
        long actualCorrelationId = Long.parseLong(lastCorrelationId.toString());
        String diagnostic = "Incorrect processing of " + Long.toHexString(expectedValue) + " in buffer {" + TestHelper.dump(packet.payload) + "}\n";
        Assert.assertEquals(diagnostic, expectedValue, actualCorrelationId);
    }

    private static JTestPacket makePacket (long value, int headerLength, int footerLength) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte [] valueBuffer = new byte[8];


        // skip header
        baos.write(TBPlayerCorrelationIdExtractor.MAGIC);
        TestHelper.writeLong(valueBuffer, 0, 12121212L);
        baos.write(valueBuffer, 0, 8);

        TestHelper.storeNoise(baos, headerLength); // store header

        baos.write(TBPlayerCorrelationIdExtractor.MAGIC);
        TestHelper.writeLong(valueBuffer, 0, value);
        baos.write(valueBuffer, 0, 8);

        TestHelper.storeNoise(baos, footerLength); // store footer
        baos.close();
        return new JTestPacket(baos.toByteArray());
    }



}
