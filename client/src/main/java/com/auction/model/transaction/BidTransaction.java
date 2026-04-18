package com.auction.model.transaction;



import com.auction.model.entity.Entity;

import java.time.LocalDateTime;

/**
 * Lớp BidTransaction - ghi lại mỗi lần đặt giá.
 * Kế thừa Entity.
 */
public class BidTransaction extends Entity {

    private static final long serialVersionUID = 1L;

    private final String auctionId;
    private final String bidderId;
    private final String bidderName;
    private final double bidAmount;
    private final double previousBid;
    private final LocalDateTime bidTime;

    public BidTransaction(String auctionId, String bidderId, String bidderName,
                          double bidAmount, double previousBid) {
        super();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.previousBid = previousBid;
        this.bidTime = LocalDateTime.now();
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public double getPreviousBid() {
        return previousBid;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    @Override
    public String printInfo() {
        return String.format("BidTransaction[auction=%s, bidder=%s, amount=%.2f, time=%s]",
                auctionId, bidderName, bidAmount, bidTime);
    }
}