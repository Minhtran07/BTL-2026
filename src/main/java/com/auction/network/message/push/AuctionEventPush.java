package com.auction.network.message.push;

/**
 * Push message gửi từ server xuống client cho sự kiện phiên đấu giá
 * (STARTED, ENDED, CANCELED, EXTENDED...).
 */
public class AuctionEventPush extends PushMessage {
    private static final long serialVersionUID = 1L;

    private final String eventType;
    private final String eventMessage;

    public AuctionEventPush(String auctionId, String eventType, String eventMessage) {
        super(auctionId);
        this.eventType = eventType;
        this.eventMessage = eventMessage;
    }

    public String getEventType() { return eventType; }
    public String getEventMessage() { return eventMessage; }
}
