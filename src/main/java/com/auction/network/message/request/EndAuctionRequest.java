package com.auction.network.message.request;

import com.auction.network.message.Request;

public class EndAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;

    public EndAuctionRequest(String auctionId) {
        super(Type.END_AUCTION);
        this.auctionId = auctionId;
        put("auctionId", auctionId);
    }

    public String getAuctionId() { return auctionId; }
}
