package com.auction.network.message.request;

import com.auction.network.message.Request;

/**
 * Request hủy subscribe push events cho 1 phiên (khi rời màn hình chi tiết).
 * Giảm tải server — không gửi push cho client không cần nữa.
 */
public class UnsubscribeAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;

    public UnsubscribeAuctionRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() { return auctionId; }
}
