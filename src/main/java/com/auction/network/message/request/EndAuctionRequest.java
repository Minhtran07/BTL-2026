package com.auction.network.message.request;

import com.auction.network.message.Request;

/** Request kết thúc phiên đấu giá thủ công (RUNNING → FINISHED + settlement). */
public class EndAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;

    public EndAuctionRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() { return auctionId; }
}
