package com.auction.network.message.request;

import com.auction.network.message.Request;

public class RegisterAutoBidRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;
    private final double maxBid;
    private final double increment;

    public RegisterAutoBidRequest(String auctionId, double maxBid, double increment) {
        super(Type.REGISTER_AUTO_BID);
        this.auctionId = auctionId;
        this.maxBid = maxBid;
        this.increment = increment;
        put("auctionId", auctionId);
        put("maxBid", String.valueOf(maxBid));
        put("increment", String.valueOf(increment));
    }

    public String getAuctionId() { return auctionId; }
    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
}
