package com.auction.service;

import com.auction.dao.GenericDao;
import com.auction.dao.UserDao;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.entity.Entity;
import com.auction.model.item.Item;
import com.auction.model.item.ItemCategory;
import com.auction.model.transaction.BidTransaction;
import com.auction.model.user.User;
import com.auction.pattern.singleton.observer.AuctionEventDispatcher;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// ============================================================================
// AUCTIONSERVICETEST - UNIT TEST CHO AUCTIONSERVICE
// ============================================================================
//
// <p>Test AuctionService - lớp service chính của domain đấu giá.
//
// <p><b>Cách ly khỏi DB:</b> dùng InMemoryDao (HashMap thay vì SQLite) để:
// <ul>
//   <li>Test nhanh (không IO disk)</li>
//   <li>Test isolated (mỗi test có DB sạch)</li>
//   <li>Test có thể chạy ở CI không cần SQLite</li>
// </ul>
//
// <p><b>Bao phủ:</b>
// <ul>
//   <li>Tạo item + auction qua Factory Pattern</li>
//   <li>Place bid: happy path + invalid + closed</li>
//   <li>Auto-bid burst</li>
//   <li>End auction + settlement (trừ winner, cộng seller)</li>
//   <li>Cancel auction</li>
// </ul>

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho AuctionService.
 *
 * <p><b>Cách ly hoàn toàn</b>: tất cả DAOs đều là in-memory — không đọc hoặc
 * ghi file nào trên đĩa.  Singleton {@link AuctionRegistry} và
 * {@link AuctionEventDispatcher} được reset trước mỗi test để tránh side-effects.
 */
class AuctionServiceTest {

    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        // Reset Singletons để tránh side effects giữa các test
        AuctionEventDispatcher.resetInstance();

        // Tất cả DAOs đều in-memory — không chạm file hệ thống
        GenericDao<Item>    itemDao    = new InMemoryDao<>();
        GenericDao<Auction> auctionDao = new InMemoryDao<>();
        UserService         userService = new UserService(new InMemoryUserDao());
        AuctionRegistry     auctionRegistry = new AuctionRegistry();

        auctionService = new AuctionService(itemDao, auctionDao, userService, auctionRegistry);
    }

    @AfterAll
    static void tearDownAll() {
        AuctionEventDispatcher.resetInstance();
    }

    // ==================== Item Management ====================

    @Test
    @DisplayName("Tạo item Electronics thành công")
    void testCreateElectronicsItem() {
        Map<String, String> extra = new HashMap<>();
        extra.put("brand", "Apple");
        extra.put("model", "iPhone 15");
        extra.put("condition", "NEW");

        Item item = auctionService.createItem(
                ItemCategory.ELECTRONICS, "iPhone 15 Pro",
                "Mới 100%", 25_000_000, "seller-1", extra);

        assertNotNull(item);
        assertEquals("iPhone 15 Pro", item.getName());
        assertEquals(ItemCategory.ELECTRONICS, item.getCategory());
    }

    @Test
    @DisplayName("Tạo item Art thành công")
    void testCreateArtItem() {
        Map<String, String> extra = new HashMap<>();
        extra.put("artist", "Picasso");
        extra.put("year", "1937");
        extra.put("medium", "Oil");

        Item item = auctionService.createItem(
                ItemCategory.ART, "Guernica Copy",
                "Bản sao nghệ thuật", 50_000_000, "seller-1", extra);

        assertNotNull(item);
        assertEquals(ItemCategory.ART, item.getCategory());
    }

    @Test
    @DisplayName("Tạo item Vehicle thành công")
    void testCreateVehicleItem() {
        Map<String, String> extra = new HashMap<>();
        extra.put("make", "Toyota");
        extra.put("vehicleModel", "Camry");
        extra.put("year", "2023");
        extra.put("mileage", "15000");

        Item item = auctionService.createItem(
                ItemCategory.VEHICLE, "Toyota Camry 2023",
                "Xe đẹp", 800_000_000, "seller-1", extra);

        assertNotNull(item);
        assertEquals(ItemCategory.VEHICLE, item.getCategory());
    }

    // ==================== Auction Management ====================

    @Test
    @DisplayName("Tạo phiên đấu giá thành công")
    void testCreateAuction() {
        Auction auction = auctionService.createAuction(
                "item-1", "seller-1", "Test Auction",
                1000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1));

        assertNotNull(auction);
        assertEquals("Test Auction", auction.getItemName());
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    @DisplayName("Tạo phiên thất bại - giá khởi điểm <= 0")
    void testCreateAuctionInvalidPrice() {
        assertThrows(IllegalArgumentException.class, () ->
                auctionService.createAuction(
                        "item-1", "seller-1", "Bad",
                        -100.0,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusHours(1)));
    }

    @Test
    @DisplayName("Tạo phiên thất bại - startTime sau endTime")
    void testCreateAuctionInvalidTime() {
        assertThrows(IllegalArgumentException.class, () ->
                auctionService.createAuction(
                        "item-1", "seller-1", "Bad",
                        1000.0,
                        LocalDateTime.now().plusHours(2),
                        LocalDateTime.now().plusHours(1)));
    }

    // ==================== Bidding ====================

    @Test
    @DisplayName("Đặt giá thành công qua service")
    void testPlaceBidViaService() throws InvalidBidException, AuctionClosedException {
        Auction auction = auctionService.createAuction(
                "item-1", "seller-1", "Bid Test",
                1000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1));

        BidTransaction tx = auctionService.placeBid(
                auction.getId(), "bidder-1", "Bidder A", 1500.0);

        assertNotNull(tx);
        assertEquals(1500.0, tx.getBidAmount());
    }

    @Test
    @DisplayName("Đặt giá thấp - ném InvalidBidException")
    void testPlaceBidTooLow() {
        Auction auction = auctionService.createAuction(
                "item-1", "seller-1", "Low Bid",
                1000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1));

        assertThrows(InvalidBidException.class, () ->
                auctionService.placeBid(auction.getId(), "bidder-1", "A", 500.0));
    }

    @Test
    @DisplayName("Đặt giá trên phiên không tồn tại")
    void testPlaceBidNonExistentAuction() {
        assertThrows(InvalidBidException.class, () ->
                auctionService.placeBid("fake-id", "bidder-1", "A", 1500.0));
    }

    // ==================== Auto-Bid via Service ====================

    @Test
    @DisplayName("Đăng ký auto-bid thành công")
    void testRegisterAutoBid() throws InvalidBidException, AuctionClosedException {
        Auction auction = auctionService.createAuction(
                "item-1", "seller-1", "Auto Test",
                1000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1));

        auctionService.placeBid(auction.getId(), "bidder-1", "A", 1100.0);

        assertDoesNotThrow(() ->
                auctionService.registerAutoBid(
                        auction.getId(), "bidder-2", "B", 2000.0, 100.0));
    }

    @Test
    @DisplayName("Auto-bid thất bại - maxBid thấp hơn giá hiện tại")
    void testRegisterAutoBidTooLow() throws InvalidBidException, AuctionClosedException {
        Auction auction = auctionService.createAuction(
                "item-1", "seller-1", "Auto Low",
                1000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1));

        auctionService.placeBid(auction.getId(), "bidder-1", "A", 1500.0);

        assertThrows(InvalidBidException.class, () ->
                auctionService.registerAutoBid(
                        auction.getId(), "bidder-2", "B", 1200.0, 100.0));
    }

    // ==================== End / Cancel ====================

    @Test
    @DisplayName("Kết thúc phiên đấu giá - xác định người thắng")
    void testEndAuction() throws InvalidBidException, AuctionClosedException {
        Auction auction = auctionService.createAuction(
                "item-1", "seller-1", "End Test",
                1000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1));

        auctionService.placeBid(auction.getId(), "bidder-1", "Winner", 2000.0);
        auctionService.endAuction(auction.getId());

        Optional<Auction> ended = auctionService.getAuction(auction.getId());
        assertTrue(ended.isPresent());
        assertEquals(AuctionStatus.FINISHED, ended.get().getStatus());
        assertEquals("Winner", ended.get().getCurrentHighestBidderName());
    }

    @Test
    @DisplayName("Hủy phiên đấu giá")
    void testCancelAuction() {
        Auction auction = auctionService.createAuction(
                "item-1", "seller-1", "Cancel Test",
                1000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1));

        auctionService.cancelAuction(auction.getId());

        Optional<Auction> canceled = auctionService.getAuction(auction.getId());
        assertTrue(canceled.isPresent());
        assertEquals(AuctionStatus.CANCELED, canceled.get().getStatus());
    }

    // ==================== Bid History ====================

    @Test
    @DisplayName("Lịch sử bid phải có đúng số lượng")
    void testBidHistory() throws InvalidBidException, AuctionClosedException {
        Auction auction = auctionService.createAuction(
                "item-1", "seller-1", "History Test",
                1000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1));

        auctionService.placeBid(auction.getId(), "b1", "A", 1100.0);
        auctionService.placeBid(auction.getId(), "b2", "B", 1200.0);
        auctionService.placeBid(auction.getId(), "b1", "A", 1500.0);

        List<BidTransaction> history = auctionService.getBidHistory(auction.getId());
        assertEquals(3, history.size());
    }

    // ==================== Concurrent Bidding ====================

    /**
     * Test concurrent bidding qua SERVICE LAYER — nơi ReentrantLock thực sự
     * bảo vệ Auction khỏi race condition.
     *
     * <p>Trước đây test này nằm ở AuctionTest (tầng Model), nhưng sau khi
     * refactoring chuyển concurrency (ReentrantLock) từ Model sang Service,
     * test phải gọi qua {@code auctionService.placeBid()} để đi qua lock.
     *
     * <p><b>Kịch bản:</b> 10 thread cùng bid đồng thời. Service phải đảm bảo:
     * <ul>
     *   <li>Không lost update (mỗi bid thành công phải cao hơn bid trước)</li>
     *   <li>Có ít nhất 1 bid thành công</li>
     *   <li>Các bid thất bại ném exception (không null return)</li>
     * </ul>
     */
    @Test
    @DisplayName("Concurrent bidding qua service: không bị lost update")
    void testConcurrentBidding() throws InterruptedException {
        Auction auction = auctionService.createAuction(
                "item-concurrent", "seller-concurrent", "Concurrent Test",
                1000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1));

        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        // Đếm số bid thành công và thất bại
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger failCount = new java.util.concurrent.atomic.AtomicInteger();

        for (int i = 0; i < numThreads; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                try {
                    // Mỗi thread bid một giá khác nhau
                    auctionService.placeBid(
                            auction.getId(),
                            "bidder-" + idx,
                            "User" + idx,
                            1000.0 + (idx + 1) * 100);
                    successCount.incrementAndGet();
                } catch (InvalidBidException | AuctionClosedException e) {
                    // Bid thất bại (giá thấp hơn do thread khác bid trước) — hợp lệ
                    failCount.incrementAndGet();
                }
            });
        }

        // Start tất cả thread đồng thời
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        // Phải có ít nhất 1 bid thành công
        assertTrue(successCount.get() > 0,
                "Phải có ít nhất 1 bid thành công, success=" + successCount.get());
        // Tổng = success + fail = numThreads (không mất thread nào)
        assertEquals(numThreads, successCount.get() + failCount.get(),
                "Tổng success + fail phải = " + numThreads);

        // Verify giá cao nhất > giá khởi điểm
        Optional<Auction> result = auctionService.getAuction(auction.getId());
        assertTrue(result.isPresent());
        assertTrue(result.get().getCurrentHighestBid() > 1000.0,
                "Giá phải cao hơn giá khởi điểm sau concurrent bidding");

        // Verify lịch sử bid tăng dần (không bị lost update)
        List<BidTransaction> history = auctionService.getBidHistory(auction.getId());
        assertTrue(history.size() > 0, "Phải có ít nhất 1 bid trong history");
        for (int i = 1; i < history.size(); i++) {
            assertTrue(history.get(i).getBidAmount() > history.get(i - 1).getBidAmount(),
                    "Mỗi bid phải cao hơn bid trước đó (no lost update)");
        }
    }

    // ==================== Regression: 2 client cùng auto-bid ====================

    /**
     * LỖI #4 — Service KHÔNG được gọi {@code auctionDao.update(auction)} N
     * lần trong vòng lặp auto-bid; chỉ 1 lần sau cả burst.
     */
    @Test
    @DisplayName("Auto-bid #4: DAO.update gọi tối đa 2 lần (1 cho manual bid + 1 cho cả burst)")
    void testDaoUpdateNotCalledPerAutoBidIteration()
            throws InvalidBidException, AuctionClosedException {
        CountingAuctionDao countingDao = new CountingAuctionDao();
        UserService userSvc = new UserService(new InMemoryUserDao());
        AuctionService svc = new AuctionService(new InMemoryDao<Item>(), countingDao, userSvc, new AuctionRegistry());

        Auction auction = svc.createAuction("item-x", "seller-x", "Burst Test",
                1000.0, LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        // Đặt 1 manual bid trước để có "leader" đang dẫn đầu.
        svc.placeBid(auction.getId(), "manual-1", "M1", 1100.0);

        // 2 auto-bidder qua lại → tạo nhiều auto-bid trong 1 burst.
        // Lúc đăng ký, mỗi lần chỉ kích hoạt một burst nhỏ.
        svc.registerAutoBid(auction.getId(), "auto-1", "A", 50_000.0, 100.0);
        svc.registerAutoBid(auction.getId(), "auto-2", "B", 50_000.0, 100.0);

        // Reset counter NGAY TRƯỚC bid manual cuối — chỉ đếm cho thao tác này.
        countingDao.resetCounter();

        // Bid manual đủ cao để vượt qua giá hiện tại (lúc này có thể là 1010 hoặc cao hơn
        // tuỳ burst đã chạy lúc đăng ký auto-bid). Dùng giá lớn đảm bảo accept.
        // Sau bid này, burst auto-bid lớn sẽ kích hoạt từ giá ~30_000+.
        Optional<Auction> snapshot = svc.getAuction(auction.getId());
        double safeBid = snapshot.get().getCurrentHighestBid() + 5_000.0;
        svc.placeBid(auction.getId(), "manual-2", "M2", safeBid);

        int updates = countingDao.getUpdateCount();
        // Kỳ vọng: 1 update cho manual bid + 1 update cho cả burst auto-bid = 2.
        // (Trước fix sẽ là 2 + N, với N = số lần auto-bid trong burst, có thể >= 10).
        assertTrue(updates <= 2,
                "DAO.update phải <= 2 lần sau fix, thực tế = " + updates);
    }

    /**
     * LỖI #1 (tầng service) — 2 client đăng ký auto-bid với increment cực
     * nhỏ; không được spin forever và phải kết thúc nhanh.
     */
    @Test
    @DisplayName("Auto-bid #1: 2 client increment 1.0 - cả luồng đăng ký + bid kết thúc nhanh")
    void testTwoClientsTinyIncrementFinishesQuickly()
            throws InvalidBidException, AuctionClosedException {
        Auction auction = auctionService.createAuction(
                "item-tiny", "seller-tiny", "Tiny Inc",
                1000.0, LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        // Đo thời gian cho TOÀN BỘ luồng: đăng ký auto-bid + manual bid kích hoạt burst.
        // Trước fix #1 (không enforce 1%), 2 client với increment=1 và maxBid=50.000 sẽ
        // tạo ~40.000 vòng lặp → mất hàng giây. Sau fix, mỗi bước >=1% nên ~370 vòng.
        long start = System.currentTimeMillis();
        auctionService.registerAutoBid(auction.getId(), "auto-1", "A", 50_000.0, 1.0);
        auctionService.registerAutoBid(auction.getId(), "auto-2", "B", 50_000.0, 1.0);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 1500,
                "Đăng ký auto-bid + burst phải kết thúc < 1.5s nhờ min 1%, thực tế = "
                        + elapsed + "ms");

        Optional<Auction> a = auctionService.getAuction(auction.getId());
        assertTrue(a.isPresent());
        // Người dẫn đầu phải là 1 trong 2 auto-bidder.
        String leader = a.get().getCurrentHighestBidderId();
        assertTrue("auto-1".equals(leader) || "auto-2".equals(leader),
                "Leader phải là 1 trong 2 auto-bidder, thực tế = " + leader);
        // Giá hiện tại phải <= maxBid (không vượt 50.000).
        assertTrue(a.get().getCurrentHighestBid() <= 50_000.0,
                "Giá không được vượt maxBid");
    }

    // ================================================================
    //  In-memory DAOs — không đọc/ghi file, hoàn toàn isolated
    // ================================================================

    /**
     * AuctionDao đếm số lần {@link #update} được gọi — dùng cho test
     * kiểm tra số lần ghi DAO.
     */
    private static class CountingAuctionDao extends InMemoryDao<Auction> {
        private final java.util.concurrent.atomic.AtomicInteger updateCount =
                new java.util.concurrent.atomic.AtomicInteger();

        @Override
        public void update(Auction entity) {
            updateCount.incrementAndGet();
            super.update(entity);
        }

        int getUpdateCount() { return updateCount.get(); }
        void resetCounter()  { updateCount.set(0); }
    }


    /**
     * DAO in-memory dùng chung cho Item, Auction và các Entity khác.
     */
    private static class InMemoryDao<T extends Entity> implements GenericDao<T> {
        private final Map<String, T> store = new ConcurrentHashMap<>();

        @Override public void save(T entity)         { store.put(entity.getId(), entity); }
        @Override public void update(T entity)       { store.put(entity.getId(), entity); }
        @Override public void delete(String id)      { store.remove(id); }
        @Override public List<T> findAll()           { return new ArrayList<>(store.values()); }
        @Override public Optional<T> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }
    }

    /** UserDao in-memory cho UserService được inject. */
    private static class InMemoryUserDao implements UserDao {
        private final Map<String, User> store = new ConcurrentHashMap<>();

        @Override public void save(User u)   { store.put(u.getId(), u); }
        @Override public void update(User u) { store.put(u.getId(), u); }
        @Override public void delete(String id) { store.remove(id); }
        @Override public List<User> findAll()   { return new ArrayList<>(store.values()); }
        @Override public Optional<User> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }
        @Override public Optional<User> findByUsername(String username) {
            return store.values().stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst();
        }
        @Override public boolean existsByUsername(String username) {
            return store.values().stream()
                    .anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
        }
        @Override public boolean existsByEmail(String email) {
            return store.values().stream()
                    .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        }
    }
}
