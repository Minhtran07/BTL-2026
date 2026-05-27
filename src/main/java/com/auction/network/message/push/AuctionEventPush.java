package com.auction.network.message.push;

import com.auction.network.message.Message;

/**
 * Push message gửi từ server xuống client cho sự kiện phiên đấu giá
 * (STARTED, ENDED, CANCELED, EXTENDED...).
 */
public class AuctionEventPush extends Message {
    private static final long serialVersionUID = 1L;

    private final String auctionId;
    private final String eventType;
    private final String eventMessage;

    public AuctionEventPush(String auctionId, String eventType, String eventMessage) {
        super(Type.AUCTION_EVENT);
        this.auctionId = auctionId;
        this.eventType = eventType;
        this.eventMessage = eventMessage;
        put("auctionId", auctionId);
        put("eventType", eventType);
        if (eventMessage != null) {
            put("message", eventMessage);
        }
    }

    public String getAuctionId() { return auctionId; }
    public String getEventType() { return eventType; }
    public String getEventMessage() { return eventMessage; }
}
