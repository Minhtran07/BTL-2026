package com.auction.model.auction;



import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Cấu hình Auto-Bidding cho một bidder trong phiên đấu giá.
 * Chức năng nâng cao: tự động trả giá khi có bid mới.
 */
public class AutoBidConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String bidderId;
    private final String bidderName;
    private final double maxBid;
    private final double increment;
    private final LocalDateTime registeredAt;

    public AutoBidConfig(String bidderId, String bidderName, double maxBid, double increment) {
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = LocalDateTime.now();
    }

    public String getBidderId() {
        return bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getMaxBid() {
        return maxBid;
    }

    public double getIncrement() {
        return increment;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    @Override
    public String toString() {
        return String.format("AutoBid[bidder=%s, maxBid=%.2f, increment=%.2f]",
                 bidderName, maxBid, increment);
    }
}