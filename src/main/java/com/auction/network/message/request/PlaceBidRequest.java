package com.auction.network.message.request;

import com.auction.network.message.Request;

/**
 * Request đặt giá — chứa auctionId + amount.
 * Handler: {@link com.auction.network.handler.PlaceBidHandler}
 * <p>Trước: {@code new Message(Type.PLACE_BID) + put("amount",…)}. Sau: typed fields.
 */
public class PlaceBidRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;
    private final double amount;

    public PlaceBidRequest(String auctionId, double amount) {
        this.auctionId = auctionId;
        this.amount = amount;
    }

    public String getAuctionId() { return auctionId; }
    public double getAmount() { return amount; }
}
