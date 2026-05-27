package com.auction.network.message.request;

import com.auction.network.message.Request;

public class UnsubscribeAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;

    public UnsubscribeAuctionRequest(String auctionId) {
        super(Type.UNSUBSCRIBE_AUCTION);
        this.auctionId = auctionId;
        put("auctionId", auctionId);
    }

    public String getAuctionId() { return auctionId; }
}
