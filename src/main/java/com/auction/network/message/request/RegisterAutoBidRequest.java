package com.auction.network.message.request;

/**
 * Request đăng ký auto-bid — chứa auctionId, giá tối đa, bước giá.
 * Handler: {@link com.auction.network.handler.RegisterAutoBidHandler}
 * <p>Server tự đặt giá mỗi khi có người bid cao hơn, cho đến khi đạt maxBid.
 */
public class RegisterAutoBidRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;
    private final double maxBid;
    private final double increment;

    public RegisterAutoBidRequest(String auctionId, double maxBid, double increment) {
        this.auctionId = auctionId;
        this.maxBid = maxBid;
        this.increment = increment;
    }

    public String getAuctionId() { return auctionId; }
    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
}
