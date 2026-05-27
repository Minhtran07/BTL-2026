package com.auction.network.message.push;

import com.auction.network.message.Message;

/**
 * Lớp cơ sở cho các push message từ server xuống client.
 *
 * <p>Mọi push event đều gắn với 1 phiên đấu giá cụ thể ({@link #auctionId}),
 * giúp client lọc event theo phiên đang xem mà không cần đọc từ data map.
 *
 * @see BidUpdatePush
 * @see AuctionEventPush
 */
public abstract class PushMessage extends Message {

    private static final long serialVersionUID = 1L;

    private final String auctionId;

    protected PushMessage(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() { return auctionId; }
}
