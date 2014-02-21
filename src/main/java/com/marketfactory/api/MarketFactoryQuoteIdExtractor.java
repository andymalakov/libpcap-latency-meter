package com.marketfactory.api;  // uses a package-local class com.marketfactory.api.MktDataMessage.

import org.jnetpcap.packet.JPacket;
import org.tinyfix.latency.protocols.CorrelationIdExtractor;
import org.tinyfix.latency.protocols.CorrelationIdListener;
import org.tinyfix.latency.util.LongFormatter;


public class MarketFactoryQuoteIdExtractor<T> implements CorrelationIdExtractor<T> {

    private final CorrelationIdListener listener;
    private final int bufferSize = 8*1024;  // Should be larger than MTU
    private final ProtoByteBuffer buffer = new ProtoByteBuffer(bufferSize, true);
    private final byte[] overflowBuffer = new byte[2*1024];  // handles messages split between packets
    private int overflowSize;
    private volatile int carryOverPacketsCounter;
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
                    if (processSingleMktMessage(packet))
                        break;
                }
            } catch (Throwable e) {
                System.err.println("Error parsing packet #" + packet.getFrameNumber() + ": " + e.getMessage());
                e.printStackTrace();
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
    private boolean processSingleMktMessage(JPacket packet) {
        assert Thread.holdsLock(overflowBuffer);

        final int msgStart = buffer.position();
        final int remainingBytes = buffer.remaining();
        if ( ! buffer.readMessageHeader()) {
            if (remainingBytes < 2) {
                System.err.println("Buffer is empty");
            } else {
                final int msgLen = buffer.msgLen;
                if (msgLen < 10) {
                    System.err.println("MsgLen specified in the message is too small: " + msgLen);
                } else if (msgLen > bufferSize) {
                    System.err.println("MsgLen specified in the message is too large: " + msgLen);
                } else if (msgLen > remainingBytes) {
                    if (overflowBuffer.length < remainingBytes)
                        throw new RuntimeException("Overflow exceeds max: " + remainingBytes + " bytes to carry over. Maximum capacity: " + overflowBuffer.length);
                    assert overflowSize == 0;
                    while (overflowSize < remainingBytes)
                        overflowBuffer[overflowSize++] = buffer.getByte();
                    carryOverPacketsCounter++;
                } else {
                    System.err.println("Unknown problem: MsgLen " + msgLen);
                }

            }
            return true; // we will interrupt packet parsing => no need to restore original position
        }

        final int msgLen = buffer.msgLen;

        final IMessage msg = Protocol.getGarbageFreeInstance(buffer.msgType, buffer);
        if(msg != null) {
            decodeMessage(packet, msg);
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

    private void decodeMessage(JPacket packet, IMessage msg) {
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
        else if (msg instanceof SubmitOrderMessage) {
            SubmitOrderMessage order = (SubmitOrderMessage) msg;
            //System.out.println("SubmitOrderMessage: " + msg);
            //System.out.println("OUT>  " + order.contents.clOrdID);
            String clOrdId = order.contents.clOrdID;
            byte [] clOrdIdBytes = clOrdId.getBytes();
            listener.onCorrelationId(packet, clOrdIdBytes, 0, clOrdIdBytes.length);
        }
//        if (msg instanceof RuThereMessage) {
//            //System.out.println("RuThereMessage: " + msg);
//        } else {
//            System.out.println("Other: " + msg.getClass().getSimpleName());
//        }
    }

    private static void skip(ProtoByteBuffer buffer, int leftover) {
        for (int i=0; i < leftover; i++)
            buffer.getByte();
    }



}

