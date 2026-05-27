package com.auction.network.message.request;

import com.auction.network.message.Request;

public class SubscribeAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;

    public SubscribeAuctionRequest(String auctionId) {
        super(Type.SUBSCRIBE_AUCTION);
        this.auctionId = auctionId;
        put("auctionId", auctionId);
    }

    public String getAuctionId() { return auctionId; }
}
