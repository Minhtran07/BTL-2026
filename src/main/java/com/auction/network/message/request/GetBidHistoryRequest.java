package com.auction.network.message.request;

/** Request lấy lịch sử bid của 1 phiên đấu giá theo auctionId. */
public class GetBidHistoryRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;

    public GetBidHistoryRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() { return auctionId; }
}
