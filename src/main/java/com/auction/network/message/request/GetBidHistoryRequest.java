package com.auction.network.message.request;

import com.auction.network.message.Request;

public class GetBidHistoryRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;

    public GetBidHistoryRequest(String auctionId) {
        super(Type.GET_BID_HISTORY);
        this.auctionId = auctionId;
        put("auctionId", auctionId);
    }

    public String getAuctionId() { return auctionId; }
}
