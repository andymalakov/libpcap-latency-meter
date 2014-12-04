package org.tinyfix.latency.protocols;

import org.jnetpcap.packet.JPacket;
import org.junit.Assert;
import org.junit.Test;
import org.tinyfix.latency.protocols.fix.FixMessageTagExtractor;

/**
 * @author Andy
 *         Date: 2/5/14
 */
public class Test_FixMessageTagExtractor {

    private static final int TAG = 8888;
    private final TestCorrelationIdListener listener = new TestCorrelationIdListener();
    private final FixMessageTagExtractor extractor = new FixMessageTagExtractor(TAG, listener);


    private static String SAMPLE_FIX_MESSAGE="8=FIX.4.3\u00019=519\u000135=W\u000134=7484\u000149=price.CACIB\u000152=20140205-23:27:50.254\u000156=demo.price\u000155=USD/CHF\u0001262=938041858695308\u00018888=1391642878207877000\u0001268=6\u0001269=0\u0001270=0.90367\u000115=USD\u0001271=1000000\u0001276=A\u0001282=1\u0001299=89708\u0001290=0\u0001269=0\u0001270=0.90357\u000115=USD\u0001271=1000000\u0001276=A\u0001282=1\u0001299=89709\u0001290=0\u0001269=0\u0001270=0.90347\u000115=USD\u0001271=1000000\u0001276=A\u0001282=1\u0001299=89710\u0001290=0\u0001269=1\u0001270=0.90405\u000115=USD\u0001271=1000000\u0001276=A\u0001282=1\u0001299=89711\u0001290=0\u0001269=1\u0001270=0.90415\u000115=USD\u0001271=1000000\u0001276=A\u0001282=1\u0001299=89712\u0001290=0\u0001269=1\u0001270=0.90425\u000115=USD\u0001271=1000000\u0001276=A\u0001282=1\u0001299=89713\u0001290=0\u000110=188\u0001";
    private static String SAMPLE_CORRELATION_ID="1391642878207877000";

    @Test
    public void normalParsing() {
        assertCorrelationId(SAMPLE_FIX_MESSAGE, SAMPLE_CORRELATION_ID);
    }

    @Test
    public void truncatedMessageParsing() {
        assertNoCorrelationId(SAMPLE_FIX_MESSAGE.substring(0, 5));
    }

    private void assertCorrelationId (String message, String expectedCorrelationId) {
        JTestPacket packet = new JTestPacket (message.getBytes());
        extractor.parse(packet, 0, packet.getLength(), null);
        if (listener.lastCorrelationID == null)
            Assert.fail("Failed to find correlation ID");
        Assert.assertEquals(expectedCorrelationId, listener.lastCorrelationID);
    }

    private void assertNoCorrelationId (String message) {
        JTestPacket packet = new JTestPacket (message.getBytes());
        extractor.parse(packet, 0, packet.getLength(), null);
        if (listener.lastCorrelationID != null)
            Assert.fail("Not expecting to find any correlation ID");
    }

    private static class TestCorrelationIdListener implements CorrelationIdListener {

        String lastCorrelationID;

        @Override
        public void onCorrelationId(JPacket packet, byte[] buffer, int offset, int length) {
            lastCorrelationID = new String (buffer, offset, length);
        }
    }
}
