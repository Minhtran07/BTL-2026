package com.auction.pattern.strategy;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;

/**
 * Chiến lược xác thực giá đấu có giá sàn (reserve price).
 *
 * <p>Mở rộng các kiểm tra của {@link StandardBidValidation} bằng cách thêm
 * điều kiện: giá bid phải đạt hoặc vượt một mức sàn tối thiểu do người bán đặt ra.
 * Nếu không đạt giá sàn, bid bị từ chối ngay cả khi cao hơn giá hiện tại.
 *
 * <p><b>Ví dụ sử dụng:</b>
 * <pre>{@code
 * // Người bán yêu cầu giá sàn 5,000,000 VNĐ
 * BidValidationStrategy strategy = new ReservePriceBidValidation(5_000_000);
 * strategy.validate(auction, bidderId, 4_500_000); // → ném exception
 * strategy.validate(auction, bidderId, 5_100_000); // → hợp lệ
 * }</pre>
 *
 * <p>Đây là một triển khai thứ hai của {@link BidValidationStrategy}, minh hoạ
 * khả năng thay đổi chiến lược xác thực linh hoạt (Strategy Pattern).
 */
public class ReservePriceBidValidation implements BidValidationStrategy {

    private static final double MIN_INCREMENT_PERCENT = 0.01; // 1%, giống StandardBidValidation

    private final double reservePrice;

    /**
     * @param reservePrice giá sàn tối thiểu (phải ≥ 0)
     */
    public ReservePriceBidValidation(double reservePrice) {
        if (reservePrice < 0) {
            throw new IllegalArgumentException("Reserve price phải không âm");
        }
        this.reservePrice = reservePrice;
    }

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

        // 6. Kiểm tra giá sàn — điểm khác biệt so với StandardBidValidation
        if (bidAmount < reservePrice) {
            throw new IllegalArgumentException(
                    String.format("Giá đấu (%.2f) phải đạt giá sàn tối thiểu (%.2f)",
                            bidAmount, reservePrice));
        }
    }

    public double getReservePrice() {
        return reservePrice;
    }
}