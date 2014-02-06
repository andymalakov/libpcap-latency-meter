package org.tinyfix.latency.protocols;

import com.marketfactory.api.MarketFactoryQuoteIdExtractor;
import org.jnetpcap.packet.JPacket;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Test_MarketFactoryQuoteIdExtractor {

    private final TestCorrelationIdListener listener = new TestCorrelationIdListener();
    private final MarketFactoryQuoteIdExtractor extractor = new MarketFactoryQuoteIdExtractor(listener);

    @Test
    public void singleMessagePerPacket1() {
        String payload = "650099b02079982b1bcf4f13044680faaab8e5c6e7cf2690a1b5b9e5c6e7cf26d0cabcb9e5c6e7cf2600d0b5c3b9e5c6e7cf26000002c0b0eb91148080a0e3a4fdb51c010101010200c0b1be9214808088fccdbcc32301010201060001044694d5ad4f94d5ad4f";
        assertCorrelationId(payload, "1391558525483249000");
    }

    @Test
    public void singleMessagePerPacket2() {
        String payload = "bb0099b0c8bb292d1bcf4f13324680e490d1e5c6e7cf26a08bb5d2e5c6e7cf26d0c5c6d2e5c6e7cf2600f0cfcdd2e5c6e7cf2600000880b6a4a9168080e898a9bf8d07000101010200c0fda1a9160002010101000080ccf3a8168080e898a9bf8d07000101010200c093f1a816000201010100008092adaa1600020102010000c0caafaa168080e898a9bf8d0700010201020080fcddaa1600020102010000c0b4e0aa168080bcdf9eebab10010102010400013246b6d5ad4fb6d5ad4f";
        assertCorrelationId(payload, "1391558525509547000");
    }
    @Test
    public void multipleMessagesPerPacket() {
        String largePacket1 = "4b0099b0e0bcbf2320cf4f13324a80a4ebb584c8e7cf26c0a6edbb84c8e7cf26d0d3f8bb84c8e7cf2600a0d4fdbb84c8e7cf2600000180dcc1a9160002010101000001324abad6ac41bad6ac414b0099b03855c02320cf4f13324a80a4ebb584c8e7cf26c0a6edbb84c8e7cf26e095fdbb84c8e7cf2600d0b582bc84c8e7cf26000001c0a492aa160002010201000001324abcd6ac41bcd6ac41520099b060f5c02320cf4f13324a80a4ebb584c8e7cf26c0a6edbb84c8e7cf2690f781bc84c8e7cf2600a0b687bc84c8e7cf2600000180dcc1a9168080e898a9bf8d0700010101020001324abed6ac41bed6ac41520099b0f04ec32320cf4f13324a80a4ebb584c8e7cf26c0a6edbb84c8e7cf26e0f786bc84c8e7cf2600c09c9abc84c8e7cf2600000180ec8faa168080e898a9bf8d0700010201020001324ac0d6ac41c0d6ac414b0099b02899c32320cf4f13324a80a4ebb584c8e7cf26c0a6edbb84c8e7cf2680888cbc84c8e7cf2600b0c59cbc84c8e7cf26000001c0c889a9160002010101000001324ac2d6ac41c2d6ac41520099b050c7d62320cf4f13324680e3c1bc84c8e7cf26a0fd96bd84c8e7cf26b080b0bd84c8e7cf260080feb5bd84c8e7cf2600000180ebbca9168080e898a9bf8d07010101010200013246f4ffae4ff4ffae4f840099b05807da2320cf4f130a4680e3c1bc84c8e7cf2680d4c3bd84c8e7cf26b0afcabd84c8e7cf260090fecfbd84c8e7cf26000004c0b89ea30d8080e898a9bf8d0700010101020080809ca30d8080e898a9bf8d070101010102008088c3a30d00020102010000c0c0c5a30d8080b8cafbbda815010102010400010a46f8ffae4ff8ffae4f520099b000aae42320cf4f13324680ecbbbd84c8e7cf2680cf98be84c8e7cf26f0eb9ebe84c8e7cf2600e088a5be84c8e7cf260000018098aea9168080e898a9bf8d07000101010200013246faffae4ffaffae4f5e0099b0c8acec2320cf4f13144680ecbbbd84c8e7cf26d09bd8be84c8e7cf2680f7debe84c8e7cf2600f093e5be84c8e7cf26000002c0cd9caa180002010101000080959aaa188080b8cafbbda815010101010400011446feffae4ffeffae4f840099b0f81bee2320cf4f13144680ecbbbd84c8e7cf26d09bd8be84c8e7cf26c094eabe84c8e7cf260080c1f0be84c8e7cf2600000480e8a8aa188080e898a9bf8d07010101010200c0afa6aa188080d0b1d2fe9a0e00010101020080b390aa1800020101010000c0fa8daa188080d0b1d2fe9a0e0001010102000114468080af4f8080af4f530099b04000442420cf4f13024680a298c384c8e7cf26908290c484c8e7cf26b08998c484c8e7cf2600e0e19fc484c8e7cf2600000180eedfb5ea0b8080e898a9bf8d070001010102000102469080af4f9080af4f520099b0600b642420cf4f13044680b48cc584c8e7cf2690828ac684c8e7cf26f0af9ac684c8e7cf2600f09da0c684c8e7cf260000018096df91148080c0c6c9faeb380101010106000104469c80af4f9c80af4f530099b0a059712420cf4f13024680bd86c684c8e7cf26909dfec684c8e7cf26908b84c784c8e7cf2600a0c78ac784c8e7cf2600000180f0bba2eb0b8080d0b1d2fe9a0e010102010400010246a680af4fa680af4f4b0099b0403f782420cf4f13324680bd86c684c8e7cf26e0a2abc784c8e7cf26d0a4bac784c8e7cf260090cec1c784c8e7cf2600000180ebbca91600020101010000013246b080af4fb080af4f650099b020df7a2420cf4f13044680bd86c684c8e7cf26e0a2abc784c8e7cf2680f1c5c784c8e7cf2600a0ddd6c784c8e7cf2600000280d39e92148080e898a9bf8d07000102010200c0fca592148080b8cafbbda815010102010600010446b280af4fb280af4f4b0099b0d8de7c2420cf4f13144680bd86c684c8e7cf26b0abd7c784c8e7cf26e089ddc784c8e7cf260090dce6c784c8e7cf26000001c0d4f0a91800020101010000011446b680af4fb680af4f520099b01088822420cf4f13084680c680c7";
        assertMultiCorrelationId(largePacket1, "1391558546826442000, 1391558546826481000, 1391558546826522000, 1391558546826676000, 1391558546826695000, 1391558546827952000, 1391558546828165000, 1391558546828862000, 1391558546829387000, 1391558546829480000, 1391558546835110000, 1391558546837211000, 1391558546838082000, 1391558546838533000, 1391558546838706000, 1391558546838837000");

        // this packet starts with tail of market message from previous packet
        String largePacket2 = "84c8e7cf26b0abd7c784c8e7cf2680968dc884c8e7cf2600d09393c884c8e7cf2600000180ebd4bc0d8080b8cafbbda815010102010400010846c080af4fc080af4f520099b068f6892420cf4f13324680c680c784c8e7cf26a0ff9ec884c8e7cf26d0a1b8c884c8e7cf2600b0bacfc884c8e7cf26000001c0c1b5a9168080e48386928a0c010101010400013246c880af4fc880af4f520099b030118e2420cf4f13144680c680c784c8e7cf26a0ff9ec884c8e7cf26d088eac884c8e7cf2600c0a5f0c884c8e7cf2600000180abe9a9188080e898a9bf8d07000101010200011446d280af4fd280af4f520099b0c0b0ae2420cf4f13324680c680c784c8e7cf2690f4d5ca84c8e7cf26e0b5efca84c8e7cf2600e0a3f5ca84c8e7cf26000001c0c1b5a9168080cc9cafd19713010101010600013246da80af4fda80af4f4b0099b0c8fcaf2420cf4f13324680c680c784c8e7cf2690f4d5ca84c8e7cf26c0f8f8ca84c8e7cf2600f0d3ffca84c8e7cf260000018098aea91600020101010000013246dc80af4fdc80af4f150015a060226f2520cf4f13f082f6d684c8e7cf26000e8b0099b0e003bd2520cf4f130446808384db84c8e7cf26f0acd9db84c8e7cf26e0c6e0db84c8e7cf2600d0e0e7db84c8e7cf26000004c0b0eb91148080f890c5b8944e0101010104008096df91148080a8dff2b9f93f010101010800c0deaf92148080b8cafbbda81501010201040080f9bb92148080d0b1d2fe9a0e010102010200010446fa80af4ffa80af4f520099b0f095c02520cf4f13044a808384db84c8e7cf269084f1db84c8e7cf26b0f9fedb84c8e7cf2600c0b884dc84c8e7cf2600000180a5da91148080e898a9bf8d0701010101020001044afcd6ac41fcd6ac41520099b0003ac12520cf4f13044a808384db84c8e7cf269084f1db84c8e7cf26f0a884dc84c8e7cf2600e0c889dc84c8e7cf2600000180dbc592148080e898a9bf8d0701010201020001044afed6ac41fed6ac41520099b0c8e9c12520cf4f13044a808384db84c8e7cf269084f1db84c8e7cf2690b989dc84c8e7cf2600f0878fdc84c8e7cf2600000180d2cb91148080e898a9bf8d0700010101020001044a80d7ac4180d7ac41520099b0a895c22520cf4f13044a808384db84c8e7cf269084f1db84c8e7cf26d0e88edc84c8e7cf260080c794dc84c8e7cf260000018088b7921480808ca8edb8e80700010201020001044a82d7ac4182d7ac41840099b02843ef2520cf4f130446809ef2dd84c8e7cf2690f8ebde84c8e7cf26d0a1f3de84c8e7cf2600e0ddf9de84c8e7cf26000004c083fa911400020101010000c092f591148080b8cafbbda815010101010600c09a9c92148080e898a9bf8d07000102010200c08ba1921480809ce5fd9ed40a010102010600010446fe80af4ffe80af4f520099b0e827182620cf4f13044680b9e0e084c8e7cf2680b7b4e184c8e7cf26e082bbe184c8e7cf2600b080c1e184c8e7cf2600000180f2e792148080e898a9bf8d070001020102000104468081af4f8081af4f520099b0282f1c2620cf4f13044680b9e0e084c8e7cf26a0f3d4e184c8e7cf269090dbe184c8e7cf2600b09de1e184c8e7cf2600000180e3ec92148080e898a9bf8d070101020102000104468281af4f8281af4f840099b090f7d62620cf4f130a4680a599ec84c8e7cf2680e9a9ed84c8e7cf26b0c4b0ed84c8e7cf260080bfb7ed84c8e7cf26000004c0b89ea30d0002010101000080809ca30d8080d0b1d2fe9a0e0101010104008088c3a30d8080e898a9bf8d07000102010200c0c0c5a30d8080d0b1d2fe9a0e010102010200010a469881af4f9881af4f4b0099b040156d2720cf4f13144680ffddf584c8e7cf26c0d2d4f684c8e7cf26b0efdaf684c8e7cf260090a6e8f684c8e7cf26000001c0e591ab18000201020100000114469e81af4f9e81af4f520099b0f0c86d2720cf4f13144680ffddf584c8e7cf26f098e2f684c8e7cf26c096e8f684c8e7cf2600c084eef684c8e7cf2600";
        assertMultiCorrelationId(largePacket2, "1391558546839201000, 1391558546839695000, 1391558546839964000, 1391558546842102000, 1391558546842187000, 1391558546859817000, 1391558546860052000, 1391558546860094000, 1391558546860139000, 1391558546860184000, 1391558546863110000, 1391558546865791000, 1391558546866055000, 1391558546878296000, 1391558546888133000");
    }

    /**
     * @param payload - payload of TCP packet, encoded as "Hex Stream" using Wireshark
     */
    private void assertCorrelationId (String payload, String expectedCorrelationId) {
        byte [] payloadAsBytes = convert2bytes(payload);

        JTestPacket packet = new JTestPacket (payloadAsBytes);
        extractor.parse(packet, 0, packet.getLength(), null);
        Assert.assertEquals(expectedCorrelationId, listener.getSingleCorrelationID());
    }

    /**
     * @param payload - payload of TCP packet, encoded as "Hex Stream" using Wireshark
     */
    private void assertMultiCorrelationId (String payload, String expectedCorrelationIds) {
        byte [] payloadAsBytes = convert2bytes(payload);

        JTestPacket packet = new JTestPacket (payloadAsBytes);
        listener.reset();
        extractor.parse(packet, 0, packet.getLength(), null);
        String [] expectedCorrelationIdsArray = expectedCorrelationIds.split(", ");


        Assert.assertEquals("Same number of correlation Ids", expectedCorrelationIdsArray.length, listener.lastCorrelations.size());
        for (int i = 0; i < expectedCorrelationIdsArray.length; i++)
            Assert.assertEquals(expectedCorrelationIdsArray[i], listener.getCorrelationID(i));
    }

    private static byte[] convert2bytes(final String payload) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (int i=0; i < payload.length(); i+=2) {
             char hi = payload.charAt(i);
             char lo = payload.charAt(i+1);
             int b = ((digit(hi) << 4) + digit(lo)) & 0xFF;
             baos.write(b);
        }
        try {
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    private static int digit(char ch) {
        if (ch >= 'a' && ch <= 'f')
            return (ch - 'a') + 10;
        if (ch >= '0' && ch <= '9')
            return ch - '0';
        throw new IllegalArgumentException("Unsupported char: 0x" + Integer.toHexString(ch));
    }

//    private void assertNoCorrelationId (String message) {
//        JTestPacket packet = new JTestPacket (message.getBytes());
//        extractor.parse(packet, 0, packet.getLength(), null);
//        if (listener.lastCorrelationID != null)
//            Assert.fail("Not expecting to find any correlation ID");
//    }

    private static class TestCorrelationIdListener implements CorrelationIdListener {

        private List<String> lastCorrelations = new ArrayList<>();

        @Override
        public void onCorrelationId(JPacket packet, byte[] buffer, int offset, int length) {
            lastCorrelations.add(new String (buffer, offset, length));
        }

        public String getSingleCorrelationID() {
            if (lastCorrelations.isEmpty())
                Assert.fail ("Didn't find any correlation IDs in the last packet");
            if (lastCorrelations.size() > 1)
                Assert.fail("Found more than once correlation IDs in the last packet");
            return lastCorrelations.get(0);
        }

        public String getCorrelationID(int index) {
            return lastCorrelations.get(index);
        }

        public void reset() {
            lastCorrelations.clear();
        }
    }
}
