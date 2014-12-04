package com.marketfactory.zerogc.api;  // uses a package-local class com.marketfactory.api.MktDataMessage.

import org.jnetpcap.packet.JPacket;
import org.tinyfix.latency.protocols.CorrelationIdExtractor;
import org.tinyfix.latency.protocols.CorrelationIdListener;
import org.tinyfix.latency.util.LongFormatter;


public class MarketFactoryQuoteIdExtractor<T> implements CorrelationIdExtractor<T> {
    private static final boolean VERBOSE = false;
    private final CorrelationIdListener listener;
    private final int bufferSize = 8*1024;  // Should be larger than MTU
    private final ZeroGCProtoByteBuffer buffer = new ZeroGCProtoByteBuffer(bufferSize, true);
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

        if (overflowSize > 0) { // If previous packet had some leftovers lets pour them in first
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

        try {
            decodeMessage(packet);
        } catch(Exception e) {
            System.err.println("MF Decoder: Could not decode MsgLen:" + buffer.msgLen + ", MsgType:" + buffer.msgType);
            buffer.clear();
            return true;
        }

        int leftover = (msgStart + msgLen) - buffer.position() + 2;
        if (leftover > 0)
            skip(buffer, leftover);
        return false;
    }

    private void decodeMessage(JPacket packet) {
        // In-lining Protocol.recycleZeroGCInstance(buffer.msgType, buffer) to avoid decoding unwanted messages (like frequent heartbeats)
        switch (buffer.msgType) {
            case MktDataMessage.ID:
                decodeMarketDataMessage(packet);
                break;
            case SubmitOrderMessage.ID:
                decodeSubmitOrderMessage(packet);
                break;
            case OrderSubmittedMessage.ID:
                decodeOrderSubmittedMessage(packet);
                break;
            case OrderRejectedMessage.ID:
                decodeOrderRejectedMessage(packet);
                break;
            case OrderReceivedMessage.ID:
                decodeOrderReceivedMessage(packet);
                break;
            case TradeCaptureMessage.ID:
                decodeTradeCaptureMessage(packet);
                break;
//            case RuThereMessage.ID:
//            case HeartbeatMessage.ID:
//            default:
//                  System.out.println("Other: " + msg.getClass().getSimpleName());
        }
    }

    private static final MktDataMessage mktDataMessage = new MktDataMessage(); //MktDataMessage.recycleZeroGCInstance();
    private void decodeMarketDataMessage(JPacket packet) {
        mktDataMessage.deserializeZeroGC(buffer);
        if (VERBOSE)
            System.out.println("MktDataMessage: " + mktDataMessage);
        LongFormatter.format(mktDataMessage.mvd.timeApiServer, formattedNumber, 0);
        int offset = 0;
        while (formattedNumber[offset] == ' ')
            offset++;

        listener.onCorrelationId(packet, formattedNumber, offset, formattedNumber.length - offset);
    }

    private static final SubmitOrderMessage submitOrderMessage = new SubmitOrderMessage(); //SubmitOrderMessage.recycleZeroGCInstance();
    private void decodeSubmitOrderMessage(JPacket packet) {
        submitOrderMessage.deserializeZeroGC(buffer);
        if (VERBOSE)
            System.out.println("SubmitOrderMessage: " + submitOrderMessage.contents.clOrdID + " time: " + packet.getCaptureHeader().timestampInMicros());
        int len = copyAsByteArray(submitOrderMessage.contents.clOrdID, formattedNumber);
        listener.onCorrelationId(packet, formattedNumber, 0, len);
    }

    private static final OrderReceivedMessage orderReceivedMessage = new OrderReceivedMessage(); //OrderReceivedMessage.recycleZeroGCInstance();
    private void decodeOrderReceivedMessage(JPacket packet) {
        orderReceivedMessage.deserializeZeroGC(buffer);
        if (VERBOSE)
            System.out.println("OrderReceivedMessage: " + orderReceivedMessage.contents.clOrdID + " time: " + packet.getCaptureHeader().timestampInMicros());
        int len = copyAsByteArray(orderReceivedMessage.contents.clOrdID, formattedNumber);
        listener.onCorrelationId(packet, formattedNumber, 0, len);
    }

    private static final OrderRejectedMessage orderRejectedMessage = new OrderRejectedMessage(); //OrderRejectedMessage.recycleZeroGCInstance();
    private void decodeOrderRejectedMessage(JPacket packet) {
        orderRejectedMessage.deserializeZeroGC(buffer);
        if (VERBOSE)
            System.out.println("OrderRejectedMessage: " + orderRejectedMessage.contents.clOrdID + " time: " + packet.getCaptureHeader().timestampInMicros());
        int len = copyAsByteArray(orderRejectedMessage.contents.clOrdID, formattedNumber);
        listener.onCorrelationId(packet, formattedNumber, 0, len);
    }

    private static final OrderSubmittedMessage orderSubmittedMessage = new OrderSubmittedMessage(); //OrderSubmittedMessage.recycleZeroGCInstance();
    private void decodeOrderSubmittedMessage(JPacket packet) {
        orderSubmittedMessage.deserializeZeroGC(buffer);
        if (VERBOSE)
            System.out.println("OrderSubmittedMessage: " + orderSubmittedMessage.contents.clOrdID + " time: " + packet.getCaptureHeader().timestampInMicros());
        int len = copyAsByteArray(orderSubmittedMessage.contents.clOrdID, formattedNumber);
        listener.onCorrelationId(packet, formattedNumber, 0, len);
    }

    private static final TradeCaptureMessage tradeCaptureMessage = new TradeCaptureMessage(); //TradeCaptureMessage.recycleZeroGCInstance();
    private void decodeTradeCaptureMessage(JPacket packet) {
        tradeCaptureMessage.deserializeZeroGC(buffer);
        if (VERBOSE)
            System.out.println("TradeCaptureMessage: " + tradeCaptureMessage.contents.clOrdID + " time: " + packet.getCaptureHeader().timestampInMicros());
        int len = copyAsByteArray(tradeCaptureMessage.contents.clOrdID, formattedNumber);
        listener.onCorrelationId(packet, formattedNumber, 0, len);
    }

    private int copyAsByteArray(MFString clOrdId, byte[] formattedNumber) {
        final int len = clOrdId.length();
        for (int i=0; i < len; i++)
            formattedNumber[i] = (byte) clOrdId.charAt(i);
        return len;
    }

    private static void skip(ZeroGCProtoByteBuffer buffer, int leftover) {
        for (int i=0; i < leftover; i++)
            buffer.getByte();
    }

    @Override
    public String toString() {
        return "MarketFactory Data/Trade";
    }

}

