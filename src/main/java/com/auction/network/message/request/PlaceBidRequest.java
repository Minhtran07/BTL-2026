package com.auction.network.message.request;

import com.auction.network.message.Request;

public class PlaceBidRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;
    private final double amount;

    public PlaceBidRequest(String auctionId, double amount) {
        super(Type.PLACE_BID);
        this.auctionId = auctionId;
        this.amount = amount;
        put("auctionId", auctionId);
        put("amount", String.valueOf(amount));
    }

    public String getAuctionId() { return auctionId; }
    public double getAmount() { return amount; }
}
