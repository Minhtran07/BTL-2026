package com.auction.network.message.request;

/** Request lấy 1 phiên đấu giá theo ID. Handler dùng cache-first (Cache-Aside pattern). */
public class GetAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;

    public GetAuctionRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() { return auctionId; }
}
