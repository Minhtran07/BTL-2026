package com.auction.pattern.strategy;

import com.auction.model.auction.Auction;

/**
 * Strategy Pattern - Chiến lược xác thực giá đấu.
 *
 * <p>Phương thức {@link #validate} ném {@link IllegalArgumentException} khi
 * giá đấu không hợp lệ và kết thúc bình thường khi hợp lệ.  Kiểu trả về là
 * {@code void} để phản ánh đúng ngữ nghĩa: "throw on invalid, return on valid".
 *
 * <p>Các chiến lược hiện có:
 * <ul>
 *   <li>{@link StandardBidValidation} — kiểm tra tiêu chuẩn với bước giá tối thiểu 1%.</li>
 *   <li>{@link ReservePriceBidValidation} — thêm yêu cầu giá sàn (reserve price).</li>
 * </ul>
 */
public interface BidValidationStrategy {

    /**
     * Xác thực giá đấu.
     *
     * @param auction   phiên đấu giá
     * @param bidderId  ID người đặt giá
     * @param bidAmount số tiền đặt
     * @throws IllegalArgumentException nếu không hợp lệ (mô tả lý do trong message)
     */
    void validate(Auction auction, String bidderId, double bidAmount);
}
