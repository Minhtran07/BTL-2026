package com.auction.pattern.strategy;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;

/**
 * xác thực giá đấu tiêu chuẩn.
 *
 * <p>Các điều kiện kiểm tra (theo thứ tự):
 * <ol>
 *   <li>Phiên đang ở trạng thái RUNNING.</li>
 *   <li>Phiên chưa hết thời gian.</li>
 *   <li>Người đặt không phải seller của phiên.</li>
 *   <li>Giá đặt cao hơn giá hiện tại.</li>
 *   <li>Bước giá đạt tối thiểu {@value #MIN_INCREMENT_PERCENT}% của giá hiện tại.</li>
 * </ol>
 */
public class StandardBidValidation implements BidValidationStrategy {

    /** Bước giá tối thiểu tính theo tỷ lệ của giá hiện tại. */
    private static final double MIN_INCREMENT_PERCENT = 0.01; // 1%

    @Override
    public void validate(Auction auction, String bidderId, double bidAmount) {

        // 1. Kiểm tra phiên đang hoạt động
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            throw new IllegalArgumentException("Phiên đấu giá chưa bắt đầu hoặc đã kết thúc");
        }

        // 2. Kiểm tra chưa hết thời gian
        if (auction.isExpired()) {
            throw new IllegalArgumentException("Phiên đấu giá đã hết thời gian");
        }

        // 3. Kiểm tra không phải seller
        if (bidderId.equals(auction.getSellerId())) {
            throw new IllegalArgumentException("Người bán không được tự đấu giá sản phẩm của mình");
        }

        // 4. Kiểm tra giá cao hơn giá hiện tại
        if (bidAmount <= auction.getCurrentHighestBid()) {
            throw new IllegalArgumentException(
                    String.format("Giá đấu (%.2f) phải cao hơn giá hiện tại (%.2f)",
                            bidAmount, auction.getCurrentHighestBid()));
        }

        // 5. Kiểm tra bước giá tối thiểu
        double minIncrement = auction.getCurrentHighestBid() * MIN_INCREMENT_PERCENT;
        if (bidAmount - auction.getCurrentHighestBid() < minIncrement) {
            throw new IllegalArgumentException(
                    String.format("Bước giá tối thiểu là %.2f", minIncrement));
        }
    }
}