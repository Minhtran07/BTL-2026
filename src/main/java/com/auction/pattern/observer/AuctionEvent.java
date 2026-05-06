package com.auction.pattern.observer;

import com.auction.model.transaction.BidTransaction;

/**
 * Sự kiện đấu giá - dùng trong Observer Pattern.
 */
public class AuctionEvent {

    public enum EventType {
        NEW_BID,
        AUCTION_STARTED,
        AUCTION_ENDED,
        AUCTION_EXTENDED,
        AUTO_BID,
        AUCTION_CANCELED
    }

    private final EventType type;
    private final String auctionId;
    private final BidTransaction transaction;
    private final String message;

    public AuctionEvent(EventType type, String auctionId, BidTransaction transaction, String message) {
        this.type = type;
        this.auctionId = auctionId;
        this.transaction = transaction;
        this.message = message;
    }

    public AuctionEvent(EventType type, String auctionId, String message) {
        this(type, auctionId, null, message);
    }

    public EventType getType() {
        return type;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public BidTransaction getTransaction() {
        return transaction;
    }

    public String getMessage() {
        return message;
    }
}