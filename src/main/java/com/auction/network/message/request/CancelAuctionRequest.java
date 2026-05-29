package com.auction.network.message.request;

import com.auction.network.message.Request;

/** Request hủy phiên đấu giá (→ CANCELED, không settlement). */
public class CancelAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;

    public CancelAuctionRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() { return auctionId; }
}
