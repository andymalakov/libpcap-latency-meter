package com.marketfactory.api;  // uses a package-local class com.marketfactory.api.MktDataMessage.

import org.jnetpcap.packet.JPacket;
import org.tinyfix.latency.protocols.CorrelationIdExtractor;
import org.tinyfix.latency.protocols.CorrelationIdListener;
import org.tinyfix.latency.util.LongFormatter;


public class MarketFactoryQuoteIdExtractor<T> implements CorrelationIdExtractor<T> {

    private final CorrelationIdListener listener;
    private final int bufferSize = 64*1024;  // Should be larger than MTU
    private final ProtoByteBuffer buffer = new ProtoByteBuffer(bufferSize, true);
    private final byte[] overflowBuffer = new byte[1024];  // handles messages split between packets
    private int overflowSize;

    private final byte [] formattedNumber = new byte [LongFormatter.WIDTH];

    public MarketFactoryQuoteIdExtractor(CorrelationIdListener listener) {
        this.listener = listener;
    }

    @Override
    public void parse(JPacket packet, int start, int len, T cookie) {
        synchronized (overflowBuffer) {
            try {

                fillProtoBuffer(packet, start, len);

                while (buffer.remaining() > 0) {

                    if (carryOverMessageIfNecessary())
                        break;

                    if (processSingleMktMessage(packet))
                        break;
                }
            } catch (Throwable e) {
                System.err.println("Error parsing packet #" + packet.getFrameNumber() + ": " + e.getMessage());
                //e.printStackTrace();
            }
        }
    }

    private void fillProtoBuffer(JPacket packet, int start, int len) {
        assert Thread.holdsLock(overflowBuffer);
        assert len > 0;
        buffer.clear();

        if (overflowSize > 0) { // If previos packet had some leftovers lets pour them in first
            buffer.byteBuffer.put(overflowBuffer, 0, overflowSize);
            overflowSize = 0;
        }

        packet.transferTo(buffer.byteBuffer, start, len);
        buffer.flip();
    }

    /** @return true if we want to skip further processing of the packet */
    private boolean carryOverMessageIfNecessary() {
        assert Thread.holdsLock(overflowBuffer);

        final int remainingBytes = buffer.remaining();
        buffer.byteBuffer.mark();
        int msgLen = (buffer.byteBuffer.getShort() & 0xFFFF);
        buffer.byteBuffer.reset();

        if (remainingBytes < msgLen) {
            if (overflowBuffer.length < remainingBytes)
                throw new RuntimeException("Overflow exceeds max: " + remainingBytes + " bytes to carry over. Maximum capacity: " + overflowBuffer.length);
            assert overflowSize == 0;
            while (overflowSize < remainingBytes)
                overflowBuffer[overflowSize++] = buffer.getByte();
            return true;
        }
        return false;
    }

    /** @return true if we want to skip further processing of the packet */
    private boolean processSingleMktMessage(JPacket packet) {
        assert Thread.holdsLock(overflowBuffer);


        final int msgStart = buffer.position();
        if ( ! buffer.readMessageHeader()) {
            diagnoseBadMessageHeader();
            return true;
        }

        final int msgLen = buffer.msgLen;

        final IMessage msg = Protocol.getGarbageFreeInstance(buffer.msgType, buffer);
        if(msg != null) {
            if (msg instanceof MktDataMessage) {
                MktDataMessage mktDataMessage = (MktDataMessage) msg;
                //System.out.println("MktDataMessage: " + mktDataMessage);
                //System.out.println("IN>  " + mktDataMessage.mvd.timeApiServer);
                LongFormatter.format(mktDataMessage.mvd.timeApiServer, formattedNumber, 0);
                int offset = 0;
                while (formattedNumber[offset] == ' ')
                    offset++;

                listener.onCorrelationId(packet, formattedNumber, offset, formattedNumber.length - offset);
            }
        } else {
            System.err.println("MF Decoder: Could not decode MsgLen:" + buffer.msgLen + ", MsgType:" + buffer.msgType);
            buffer.clear();
            return true;
        }

        int leftover = (msgStart + msgLen) - buffer.position() + 2;
        if (leftover > 0)
            skip(buffer, leftover);
        return false;
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

