package com.auction.network.message.request;

import com.auction.network.message.Request;

public class GetAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;

    public GetAuctionRequest(String auctionId) {
        super(Type.GET_AUCTION);
        this.auctionId = auctionId;
        put("auctionId", auctionId);
    }

    public String getAuctionId() { return auctionId; }
}
