package com.auction.model.auction;

import com.auction.model.transaction.BidTransaction;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho lớp Auction.
 * Kiểm tra logic đặt giá, concurrent bidding, anti-sniping, auto-bid.
 */
class AuctionTest {

    private Auction auction;

    @BeforeEach
    void setUp() {
        auction = new Auction(
                "item-1", "seller-1", "Test Item",
                1000.0,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1));
        auction.start();
    }

    // ==================== Basic Bidding ====================

    @Test
    @DisplayName("Đặt giá hợp lệ - phải thành công")
    void testPlaceBidSuccess() {
        BidTransaction tx = auction.placeBid("bidder-1", "Nguyen A", 1500.0);

        assertNotNull(tx, "Transaction không được null");
        assertEquals(1500.0, auction.getCurrentHighestBid());
        assertEquals("bidder-1", auction.getCurrentHighestBidderId());
        assertEquals("Nguyen A", auction.getCurrentHighestBidderName());
        assertEquals(1, auction.getTotalBids());
    }

    @Test
    @DisplayName("Đặt giá thấp hơn giá hiện tại - phải thất bại")
    void testPlaceBidTooLow() {
        auction.placeBid("bidder-1", "Nguyen A", 1500.0);
        BidTransaction tx = auction.placeBid("bidder-2", "Tran B", 1200.0);

        assertNull(tx, "Bid thấp hơn phải trả về null");
        assertEquals(1500.0, auction.getCurrentHighestBid());
        assertEquals(1, auction.getTotalBids());
    }

    @Test
    @DisplayName("Đặt giá bằng giá hiện tại - phải thất bại")
    void testPlaceBidEqualToCurrent() {
        auction.placeBid("bidder-1", "Nguyen A", 1500.0);
        BidTransaction tx = auction.placeBid("bidder-2", "Tran B", 1500.0);

        assertNull(tx, "Bid bằng giá hiện tại phải thất bại");
    }

    @Test
    @DisplayName("Seller không được tự đấu giá")
    void testSellerCannotBid() {
        BidTransaction tx = auction.placeBid("seller-1", "Seller", 2000.0);
        assertNull(tx, "Seller không được phép bid");
    }

    @Test
    @DisplayName("Nhiều bid liên tiếp - giá phải tăng dần")
    void testMultipleBids() {
        auction.placeBid("bidder-1", "A", 1100.0);
        auction.placeBid("bidder-2", "B", 1200.0);
        auction.placeBid("bidder-1", "A", 1500.0);

        assertEquals(1500.0, auction.getCurrentHighestBid());
        assertEquals("bidder-1", auction.getCurrentHighestBidderId());
        assertEquals(3, auction.getTotalBids());
    }

    // ==================== Auction Lifecycle ====================

    @Test
    @DisplayName("Trạng thái OPEN → RUNNING → FINISHED")
    void testAuctionLifecycle() {
        Auction newAuction = new Auction(
                "item-2", "seller-2", "Item 2",
                500.0, LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        assertEquals(AuctionStatus.OPEN, newAuction.getStatus());

        newAuction.start();
        assertEquals(AuctionStatus.RUNNING, newAuction.getStatus());

        newAuction.finish();
        assertEquals(AuctionStatus.FINISHED, newAuction.getStatus());
    }

    @Test
    @DisplayName("Trạng thái FINISHED → PAID")
    void testMarkPaid() {
        auction.finish();
        auction.markPaid();
        assertEquals(AuctionStatus.PAID, auction.getStatus());
    }

    @Test
    @DisplayName("Hủy phiên đấu giá")
    void testCancelAuction() {
        auction.cancel();
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    @Test
    @DisplayName("Không cho phép bid khi phiên chưa bắt đầu")
    void testCannotBidWhenOpen() {
        Auction openAuction = new Auction(
                "item-3", "seller-3", "Item 3",
                500.0, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
        // Status = OPEN, not RUNNING

        BidTransaction tx = openAuction.placeBid("bidder-1", "A", 600.0);
        assertNull(tx, "Không được bid khi phiên ở trạng thái OPEN");
    }

    @Test
    @DisplayName("Không cho phép bid khi phiên đã kết thúc")
    void testCannotBidWhenFinished() {
        auction.finish();

        BidTransaction tx = auction.placeBid("bidder-1", "A", 2000.0);
        assertNull(tx, "Không được bid khi phiên đã FINISHED");
    }

    // ==================== Bid History ====================

    @Test
    @DisplayName("Lịch sử đặt giá phải đúng thứ tự")
    void testBidHistory() {
        auction.placeBid("bidder-1", "A", 1100.0);
        auction.placeBid("bidder-2", "B", 1200.0);
        auction.placeBid("bidder-3", "C", 1500.0);

        List<BidTransaction> history = auction.getBidHistory();
        assertEquals(3, history.size());
        assertEquals(1100.0, history.get(0).getBidAmount());
        assertEquals(1200.0, history.get(1).getBidAmount());
        assertEquals(1500.0, history.get(2).getBidAmount());
    }

    @Test
    @DisplayName("Bid history phải immutable")
    void testBidHistoryImmutable() {
        auction.placeBid("bidder-1", "A", 1100.0);
        List<BidTransaction> history = auction.getBidHistory();
        assertThrows(UnsupportedOperationException.class, () -> history.clear());
    }

    // ==================== Anti-Sniping ====================

    @Test
    @DisplayName("Anti-sniping: gia hạn khi bid trong 30 giây cuối")
    void testAntiSniping() {
        // Tạo auction sắp hết hạn (15 giây nữa)
        Auction sniperAuction = new Auction(
                "item-4", "seller-4", "Snipe Item",
                1000.0,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusSeconds(15));
        sniperAuction.start();

        LocalDateTime originalEnd = sniperAuction.getEndTime();
        sniperAuction.placeBid("bidder-1", "Sniper", 1500.0);

        // End time phải được kéo dài
        assertTrue(sniperAuction.getEndTime().isAfter(originalEnd),
                "Thời gian kết thúc phải được gia hạn");
        assertEquals(1, sniperAuction.getSnipeExtensionCount());
    }

    @Test
    @DisplayName("Anti-sniping có thể tắt")
    void testAntiSnipingDisabled() {
        Auction noSnipeAuction = new Auction(
                "item-5", "seller-5", "No Snipe",
                1000.0,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusSeconds(15));
        noSnipeAuction.start();
        noSnipeAuction.setAntiSnipingEnabled(false);

        LocalDateTime originalEnd = noSnipeAuction.getEndTime();
        noSnipeAuction.placeBid("bidder-1", "A", 1500.0);

        assertEquals(originalEnd, noSnipeAuction.getEndTime(),
                "End time không được thay đổi khi anti-sniping tắt");
    }

    // ==================== Auto-Bidding ====================

    @Test
    @DisplayName("Auto-Bid: tự động đặt giá khi bị vượt")
    void testAutoBid() {
        AutoBidConfig config = new AutoBidConfig("bidder-1", "AutoBidder", 2000.0, 100.0);
        auction.addAutoBid(config);

        // Bidder-2 đặt giá
        auction.placeBid("bidder-2", "Manual", 1100.0);

        // Process auto-bids (trừ bidder-2 vừa đặt)
        List<BidTransaction> autoBids = auction.processAutoBids("bidder-2");

        assertFalse(autoBids.isEmpty(), "Auto-bid phải tạo ít nhất 1 transaction");
        assertEquals("bidder-1", auction.getCurrentHighestBidderId());
        assertTrue(auction.getCurrentHighestBid() > 1100.0);
    }

    @Test
    @DisplayName("Auto-Bid: không vượt quá maxBid")
    void testAutoBidMaxLimit() {
        AutoBidConfig config = new AutoBidConfig("bidder-1", "AutoBidder", 1200.0, 100.0);
        auction.addAutoBid(config);

        auction.placeBid("bidder-2", "Manual", 1150.0);
        List<BidTransaction> autoBids = auction.processAutoBids("bidder-2");

        // maxBid = 1200, current = 1150, increment = 100 → 1250 > 1200 → không auto-bid
        assertTrue(autoBids.isEmpty(), "Auto-bid không được vượt maxBid");
    }

    // ==================== Concurrent Bidding ====================

    @Test
    @DisplayName("Concurrent bidding: không bị lost update")
    void testConcurrentBidding() throws InterruptedException {
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                auction.placeBid("bidder-" + idx, "User" + idx, 1000.0 + (idx + 1) * 100);
            });
        }

        // Start all threads simultaneously
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        // Giá cao nhất phải là bid lớn nhất thành công
        assertTrue(auction.getCurrentHighestBid() > 1000.0,
                "Giá phải cao hơn giá khởi điểm sau concurrent bidding");
        assertTrue(auction.getTotalBids() > 0,
                "Phải có ít nhất 1 bid thành công");

        // Verify không có 2 bid cùng giá
        List<BidTransaction> history = auction.getBidHistory();
        for (int i = 1; i < history.size(); i++) {
            assertTrue(history.get(i).getBidAmount() > history.get(i - 1).getBidAmount(),
                    "Mỗi bid phải cao hơn bid trước đó");
        }
    }
}

