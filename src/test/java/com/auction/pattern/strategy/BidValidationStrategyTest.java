package com.auction.pattern.strategy;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * BIDVALIDATIONSTRATEGYTEST - TEST STRATEGY PATTERN
 * ============================================================================
 *
 * <p>Test cho 2 chiến lược validate giá đấu:
 * <ul>
 *   <li>{@link StandardBidValidation}: bước giá ≥ 1% giá hiện tại</li>
 *   <li>{@link ReservePriceBidValidation}: thêm yêu cầu giá ≥ giá sàn</li>
 * </ul>
 *
 * <p><b>Mục đích:</b> bao phủ branch logic của Strategy Pattern - mỗi điều
 * kiện validate cần ít nhất 1 test case đi qua (cả happy + sad path).
 *
 * <p>Strategy Pattern khiến test dễ: chỉ cần test mỗi strategy độc lập,
 * không cần test toàn bộ flow của Auction.
 */
class BidValidationStrategyTest {

    private Auction auction;

    @BeforeEach
    void setUp() {
        auction = new Auction("item-1", "seller-1", "Test Item",
                1000.0,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1));
        auction.start(); // status = RUNNING
    }

    // ==================== StandardBidValidation ====================

    @Test
    void standard_validBid_shouldPass() {
        BidValidationStrategy strategy = new StandardBidValidation();
        assertDoesNotThrow(() -> strategy.validate(auction, "bidder-1", 1100.0));
    }

    @Test
    void standard_bidEqualsCurrentPrice_shouldThrow() {
        BidValidationStrategy strategy = new StandardBidValidation();
        assertThrows(IllegalArgumentException.class,
                () -> strategy.validate(auction, "bidder-1", 1000.0));
    }

    @Test
    void standard_bidBelowMinIncrement_shouldThrow() {
        BidValidationStrategy strategy = new StandardBidValidation();
        // 1% of 1000 = 10, bid 1005 < 1010 (min increment) → fail
        assertThrows(IllegalArgumentException.class,
                () -> strategy.validate(auction, "bidder-1", 1005.0));
    }

    @Test
    void standard_sellerCannotBid() {
        BidValidationStrategy strategy = new StandardBidValidation();
        assertThrows(IllegalArgumentException.class,
                () -> strategy.validate(auction, "seller-1", 2000.0));
    }

    @Test
    void standard_auctionNotRunning_shouldThrow() {
        auction.setStatus(AuctionStatus.OPEN);
        BidValidationStrategy strategy = new StandardBidValidation();
        assertThrows(IllegalArgumentException.class,
                () -> strategy.validate(auction, "bidder-1", 2000.0));
    }

    @Test
    void standard_auctionExpired_shouldThrow() {
        auction.setEndTime(LocalDateTime.now().minusMinutes(1));
        BidValidationStrategy strategy = new StandardBidValidation();
        assertThrows(IllegalArgumentException.class,
                () -> strategy.validate(auction, "bidder-1", 2000.0));
    }

    // ==================== ReservePriceBidValidation ====================

    @Test
    void reserve_negativePrice_constructorThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReservePriceBidValidation(-100.0));
    }

    @Test
    void reserve_zeroPrice_isValidConstructor() {
        ReservePriceBidValidation s = new ReservePriceBidValidation(0.0);
        assertEquals(0.0, s.getReservePrice());
    }

    @Test
    void reserve_bidBelowReserve_shouldThrow() {
        ReservePriceBidValidation strategy = new ReservePriceBidValidation(5000.0);
        // bid 1100 cao hơn current 1000 nhưng dưới reserve 5000
        assertThrows(IllegalArgumentException.class,
                () -> strategy.validate(auction, "bidder-1", 1100.0));
    }

    @Test
    void reserve_bidAtReserve_shouldPass() {
        ReservePriceBidValidation strategy = new ReservePriceBidValidation(5000.0);
        // bid 5000 = reserve, vẫn cao hơn current 1000, đủ increment
        assertDoesNotThrow(() -> strategy.validate(auction, "bidder-1", 5000.0));
    }

    @Test
    void reserve_bidAboveReserve_shouldPass() {
        ReservePriceBidValidation strategy = new ReservePriceBidValidation(5000.0);
        assertDoesNotThrow(() -> strategy.validate(auction, "bidder-1", 6000.0));
    }

    @Test
    void reserve_sellerCannotBid() {
        ReservePriceBidValidation strategy = new ReservePriceBidValidation(5000.0);
        assertThrows(IllegalArgumentException.class,
                () -> strategy.validate(auction, "seller-1", 10000.0));
    }

    @Test
    void reserve_auctionNotRunning_shouldThrow() {
        auction.setStatus(AuctionStatus.FINISHED);
        ReservePriceBidValidation strategy = new ReservePriceBidValidation(5000.0);
        assertThrows(IllegalArgumentException.class,
                () -> strategy.validate(auction, "bidder-1", 10000.0));
    }

    @Test
    void reserve_auctionExpired_shouldThrow() {
        auction.setEndTime(LocalDateTime.now().minusMinutes(1));
        ReservePriceBidValidation strategy = new ReservePriceBidValidation(5000.0);
        assertThrows(IllegalArgumentException.class,
                () -> strategy.validate(auction, "bidder-1", 10000.0));
    }

    @Test
    void reserve_bidEqualsCurrent_shouldThrow() {
        ReservePriceBidValidation strategy = new ReservePriceBidValidation(500.0);
        // bid 1000 = current 1000 → fail (rule 4)
        assertThrows(IllegalArgumentException.class,
                () -> strategy.validate(auction, "bidder-1", 1000.0));
    }

    @Test
    void reserve_getReservePrice_returnsConstructorValue() {
        ReservePriceBidValidation strategy = new ReservePriceBidValidation(12345.67);
        assertEquals(12345.67, strategy.getReservePrice());
    }
}
