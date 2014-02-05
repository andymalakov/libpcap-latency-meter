package com.marketfactory.api;  // uses a package-local class com.marketfactory.api.MktDataMessage.

import org.jnetpcap.packet.JPacket;
import org.tinyfix.latency.protocols.CorrelationIdExtractor;
import org.tinyfix.latency.protocols.CorrelationIdListener;
import org.tinyfix.latency.util.LongFormatter;


public class MarketFactoryQuoteIdExtractor<T> implements CorrelationIdExtractor<T> {

    private final CorrelationIdListener listener;
    private final int bufferSize = 64*1024;  // Should be larger than MTU
    private final ProtoByteBuffer buffer = new ProtoByteBuffer(bufferSize, true);
    private final byte[] overflowBuffer = new byte[128];  // handles messages split between packets
    private int overflowSize;

    private final byte [] formattedNumber = new byte [LongFormatter.WIDTH];

    public MarketFactoryQuoteIdExtractor(CorrelationIdListener listener) {
        this.listener = listener;
    }

    @Override
    public void parse(JPacket packet, int start, int len, T cookie) {
        assert len > 0;
        buffer.clear();
        if (overflowSize > 0) {
            buffer.byteBuffer.put(overflowBuffer, 0, overflowSize);
            overflowSize = 0;
        }

        packet.transferTo(buffer.byteBuffer, start, len); //TODO: Figure out if we can peer JPacket body to MF ByteBuffer
        buffer.flip();
        try {
//            if (packet.getFrameNumber() == 5882)
//                System.out.println("Packet " + packet);

            while (buffer.remaining() > 0) {
                final int msgStart = buffer.position();

                {
                    final int remainingBytes = buffer.remaining();
                    buffer.byteBuffer.mark();
                    int msgLen = (buffer.byteBuffer.getShort() & 0xFFFF);
                    buffer.byteBuffer.reset();

                    if (remainingBytes < msgLen) {
                        assert overflowSize == 0;
                        while (overflowSize < remainingBytes)
                            overflowBuffer[overflowSize++] = buffer.getByte();
                        break;
                    }
                }


                if ( ! buffer.readMessageHeader()) {
                    diagnoseBadMessageHeader();
                    break;
                }

                final int msgLen = buffer.msgLen;

                final IMessage msg = Protocol.getGarbageFreeInstance(buffer.msgType, buffer);
                if(msg != null) {
                    if (msg instanceof MktDataMessage) {
                        MktDataMessage mktDataMessage = (MktDataMessage) msg;
                        //System.out.println("MktDataMessage: " + mktDataMessage);
                        synchronized (formattedNumber) {
                            LongFormatter.format(mktDataMessage.mvd.timeApiServer, formattedNumber, 0);
                            int offset = 0;
                            while (formattedNumber[offset] == ' ')
                                offset++;
                            //System.out.println("IN>  " + mktDataMessage.mvd.timeApiServer);
                            listener.onCorrelationId(packet, formattedNumber, offset, formattedNumber.length - offset);
                        }
                    }
                } else {
                    System.err.println("MF Decoder: Could not decode MsgLen:" + buffer.msgLen + ", MsgType:" + buffer.msgType);
                    buffer.clear();
                    break;
                }

                int leftover = (msgStart + msgLen) - buffer.position() + 2;
                if (leftover > 0)
                    skip(buffer, leftover);
            }
        } catch (Throwable e) {
            System.err.println("Error parsing packet #" + packet.getFrameNumber() + ": " + e.getMessage());
            //e.printStackTrace();
        }
    }

    private static void skip(ProtoByteBuffer buffer, int leftover) {
        for (int i=0; i < leftover; i++)
            buffer.getByte();
    }

    private void diagnoseBadMessageHeader() {
        if (buffer.remaining() < 2) {
            System.err.println("Buffer is empty");
        } else {

            buffer.byteBuffer.mark();

            int msgLen = (buffer.byteBuffer.getShort() & 0xFFFF);
            if (msgLen < 10)
                System.err.println("MsgLen specified in the message is too small: " + msgLen);
            else
            if (msgLen > bufferSize)
                System.err.println("MsgLen specified in the message is too large: " + msgLen);
            else
            if (buffer.remaining() < msgLen)
                System.err.println("Remaining buffer size (" +  buffer.remaining() +  ") is not enough to read entire message which is supposed to have size " + msgLen );
            else
                System.err.println("Unknown problem: MsgLen " + msgLen);
            buffer.byteBuffer.reset();
        }

    }


}

