package com.auction.network.message.push;

import com.auction.model.transaction.BidTransaction;
import com.auction.network.message.Message;

/**
 * Push message gửi từ server xuống client khi có bid mới (NEW_BID / AUTO_BID).
 * Chứa thông tin bid đã xảy ra ở phiên cụ thể.
 */
public class BidUpdatePush extends Message {
    private static final long serialVersionUID = 1L;

    private final String auctionId;
    private final String eventType;
    private final String bidderId;
    private final String bidderName;
    private final double bidAmount;
    private final String bidTime;

    public BidUpdatePush(String auctionId, String eventType, BidTransaction tx) {
        super(Type.BID_UPDATE);
        this.auctionId = auctionId;
        this.eventType = eventType;
        this.bidderId = tx != null ? tx.getBidderId() : null;
        this.bidderName = tx != null ? tx.getBidderName() : null;
        this.bidAmount = tx != null ? tx.getBidAmount() : 0;
        this.bidTime = tx != null ? tx.getBidTime().toString() : null;
        put("auctionId", auctionId);
        put("eventType", eventType);
        if (tx != null) {
            put("bidderId", tx.getBidderId());
            put("bidderName", tx.getBidderName());
            put("bidAmount", String.valueOf(tx.getBidAmount()));
            put("bidTime", tx.getBidTime().toString());
        }
    }

    public String getAuctionId() { return auctionId; }
    public String getEventType() { return eventType; }
    public String getBidderId() { return bidderId; }
    public String getBidderName() { return bidderName; }
    public double getBidAmount() { return bidAmount; }
    public String getBidTime() { return bidTime; }
}
