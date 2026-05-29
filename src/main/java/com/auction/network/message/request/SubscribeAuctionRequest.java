package com.auction.network.message.request;

import com.auction.network.message.Request;

/**
 * Request subscribe push events cho 1 phiên đấu giá (Observer Pattern).
 * Sau khi subscribe, server sẽ push BidUpdatePush / AuctionEventPush
 * cho client mỗi khi có thay đổi ở phiên này.
 */
public class SubscribeAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;

    public SubscribeAuctionRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() { return auctionId; }
}
