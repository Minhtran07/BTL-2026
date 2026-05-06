package com.auction.pattern;


import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.ItemCategory;
import com.auction.model.item.Vehicle;
import com.auction.pattern.factory.ItemFactory;
import com.auction.pattern.observer.AuctionEvent;
import com.auction.pattern.observer.AuctionEventDispatcher;
import com.auction.pattern.observer.AuctionObserver;
import com.auction.pattern.singleton.AuctionManager;
import com.auction.pattern.strategy.BidValidationStrategy;
import com.auction.pattern.strategy.ReservePriceBidValidation;
import com.auction.pattern.strategy.StandardBidValidation;
import com.auction.model.auction.Auction;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho các Design Patterns.
 * Kiểm tra Singleton, Factory Method, Observer, Strategy.
 */
class DesignPatternTest {

    @BeforeEach
    void setUp() {
        // Reset Singleton instances để tránh side effects giữa các test
        AuctionEventDispatcher.resetInstance();
    }

    @AfterAll
    static void tearDownAll() {
        // Shutdown scheduler của AuctionManager để tránh thread leak
        AuctionManager.resetInstance();
        AuctionEventDispatcher.resetInstance();
    }

    // ==================== Singleton Pattern ====================

    @Test
    @DisplayName("Singleton: AuctionManager trả về cùng instance")
    void testSingletonAuctionManager() {
        AuctionManager m1 = AuctionManager.getInstance();
        AuctionManager m2 = AuctionManager.getInstance();
        assertSame(m1, m2, "Phải là cùng một instance");
    }

    @Test
    @DisplayName("Singleton: AuctionEventDispatcher trả về cùng instance")
    void testSingletonEventDispatcher() {
        AuctionEventDispatcher d1 = AuctionEventDispatcher.getInstance();
        AuctionEventDispatcher d2 = AuctionEventDispatcher.getInstance();
        assertSame(d1, d2, "Phải là cùng một instance");
    }

    // ==================== Factory Method Pattern ====================

    @Test
    @DisplayName("Factory: tạo Electronics")
    void testFactoryCreateElectronics() {
        Map<String, String> extra = new HashMap<>();
        extra.put("brand", "Samsung");
        extra.put("model", "Galaxy S24");
        extra.put("condition", "NEW");

        Item item = ItemFactory.createItem(
                ItemCategory.ELECTRONICS, "Samsung S24", "Flagship", 20000000, "s1", extra);

        assertNotNull(item);
        assertInstanceOf(Electronics.class, item);
        assertEquals(ItemCategory.ELECTRONICS, item.getCategory());
    }

    @Test
    @DisplayName("Factory: tạo Art")
    void testFactoryCreateArt() {
        Map<String, String> extra = new HashMap<>();
        extra.put("artist", "Van Gogh");
        extra.put("year", "1889");
        extra.put("medium", "Oil");

        Item item = ItemFactory.createItem(
                ItemCategory.ART, "Starry Night Copy", "Bản sao", 100000000, "s1", extra);

        assertNotNull(item);
        assertInstanceOf(Art.class, item);
    }

    @Test
    @DisplayName("Factory: tạo Vehicle")
    void testFactoryCreateVehicle() {
        Map<String, String> extra = new HashMap<>();
        extra.put("make", "Honda");
        extra.put("vehicleModel", "Civic");
        extra.put("year", "2024");
        extra.put("mileage", "0");

        Item item = ItemFactory.createItem(
                ItemCategory.VEHICLE, "Honda Civic", "Xe mới", 700000000, "s1", extra);

        assertNotNull(item);
        assertInstanceOf(Vehicle.class, item);
    }

    @Test
    @DisplayName("Factory: category không hợp lệ ném exception")
    void testFactoryInvalidCategory() {
        assertThrows(IllegalArgumentException.class, () ->
                ItemFactory.createItem(ItemCategory.OTHER, "X", "X", 100, "s1", new HashMap<>()));
    }

    // ==================== Observer Pattern ====================

    @Test
    @DisplayName("Observer: nhận event khi subscribe")
    void testObserverReceivesEvent() {
        AuctionEventDispatcher dispatcher = AuctionEventDispatcher.getInstance();
        AtomicInteger eventCount = new AtomicInteger(0);

        AuctionObserver observer = event -> eventCount.incrementAndGet();
        dispatcher.subscribe("test-auction-obs-1", observer);

        AuctionEvent event = new AuctionEvent(
                AuctionEvent.EventType.NEW_BID, "test-auction-obs-1", "Test bid");
        dispatcher.dispatch(event);

        assertEquals(1, eventCount.get(), "Observer phải nhận được 1 event");

        // Cleanup
        dispatcher.unsubscribe("test-auction-obs-1", observer);
    }

    @Test
    @DisplayName("Observer: không nhận event của phiên khác")
    void testObserverIsolation() {
        AuctionEventDispatcher dispatcher = AuctionEventDispatcher.getInstance();
        AtomicInteger eventCount = new AtomicInteger(0);

        AuctionObserver observer = event -> eventCount.incrementAndGet();
        dispatcher.subscribe("auction-X", observer);

        // Dispatch event cho phiên khác
        dispatcher.dispatch(new AuctionEvent(
                AuctionEvent.EventType.NEW_BID, "auction-Y", "Bid cho phiên Y"));

        assertEquals(0, eventCount.get(), "Observer phiên X không nhận event phiên Y");

        // Cleanup
        dispatcher.unsubscribe("auction-X", observer);
    }

    @Test
    @DisplayName("Observer: không nhận event sau unsubscribe")
    void testObserverAfterUnsubscribe() {
        AuctionEventDispatcher dispatcher = AuctionEventDispatcher.getInstance();
        AtomicInteger eventCount = new AtomicInteger(0);

        AuctionObserver observer = event -> eventCount.incrementAndGet();

        dispatcher.subscribe("test-unsub-obs", observer);
        dispatcher.unsubscribe("test-unsub-obs", observer);

        dispatcher.dispatch(new AuctionEvent(
                AuctionEvent.EventType.NEW_BID, "test-unsub-obs", "Test"));

        assertEquals(0, eventCount.get(), "Observer đã unsubscribe không được nhận event");
    }

    @Test
    @DisplayName("Observer: global observer nhận tất cả event")
    void testGlobalObserver() {
        AuctionEventDispatcher dispatcher = AuctionEventDispatcher.getInstance();
        AtomicInteger eventCount = new AtomicInteger(0);

        AuctionObserver globalObserver = event -> eventCount.incrementAndGet();
        dispatcher.subscribeGlobal(globalObserver);

        dispatcher.dispatch(new AuctionEvent(
                AuctionEvent.EventType.NEW_BID, "auction-A", "Bid A"));
        dispatcher.dispatch(new AuctionEvent(
                AuctionEvent.EventType.AUCTION_ENDED, "auction-B", "End B"));

        assertEquals(2, eventCount.get(), "Global observer phải nhận tất cả events");

        // Cleanup
        dispatcher.unsubscribeGlobal(globalObserver);
    }

    // ==================== Strategy Pattern ====================

    // ==================== Strategy Pattern (StandardBidValidation) ====================

    @Test
    @DisplayName("Strategy: validate bid hợp lệ - kết thúc không ném exception")
    void testStrategyValidBid() {
        BidValidationStrategy strategy = new StandardBidValidation();

        Auction auction = new Auction("i1", "s1", "Test", 1000.0,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1));
        auction.start();

        // validate() trả về void: không ném exception = hợp lệ
        assertDoesNotThrow(() -> strategy.validate(auction, "bidder-1", 1500.0));
    }

    @Test
    @DisplayName("Strategy: reject bid thấp hơn giá hiện tại")
    void testStrategyLowBid() {
        BidValidationStrategy strategy = new StandardBidValidation();

        Auction auction = new Auction("i1", "s1", "Test", 1000.0,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1));
        auction.start();

        assertThrows(IllegalArgumentException.class, () ->
                strategy.validate(auction, "bidder-1", 500.0));
    }

    @Test
    @DisplayName("Strategy: reject seller tự bid")
    void testStrategySellerCannotBid() {
        BidValidationStrategy strategy = new StandardBidValidation();

        Auction auction = new Auction("i1", "s1", "Test", 1000.0,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1));
        auction.start();

        assertThrows(IllegalArgumentException.class, () ->
                strategy.validate(auction, "s1", 2000.0));
    }

    @Test
    @DisplayName("Strategy: reject bid khi phiên chưa RUNNING")
    void testStrategyNotRunning() {
        BidValidationStrategy strategy = new StandardBidValidation();

        // Phiên vẫn ở trạng thái OPEN (chưa start)
        Auction auction = new Auction("i1", "s1", "Test", 1000.0,
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));

        assertThrows(IllegalArgumentException.class, () ->
                strategy.validate(auction, "bidder-1", 1500.0));
    }

    // ==================== Strategy Pattern (ReservePriceBidValidation) ====================

    @Test
    @DisplayName("ReservePriceStrategy: bid đạt giá sàn - hợp lệ")
    void testReservePriceStrategyValid() {
        BidValidationStrategy strategy = new ReservePriceBidValidation(1200.0);

        Auction auction = new Auction("i2", "s2", "Reserve Test", 1000.0,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1));
        auction.start();

        // 1300 >= reserve 1200 và > current 1000 → hợp lệ
        assertDoesNotThrow(() -> strategy.validate(auction, "bidder-1", 1300.0));
    }

    @Test
    @DisplayName("ReservePriceStrategy: bid không đạt giá sàn - bị từ chối")
    void testReservePriceStrategyBelowReserve() {
        BidValidationStrategy strategy = new ReservePriceBidValidation(5000.0);

        Auction auction = new Auction("i2", "s2", "Reserve Test", 1000.0,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1));
        auction.start();

        // 2000 > current 1000 nhưng < reserve 5000 → bị từ chối
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                strategy.validate(auction, "bidder-1", 2000.0));
        assertTrue(ex.getMessage().contains("giá sàn"), "Lý do phải đề cập giá sàn");
    }

    @Test
    @DisplayName("ReservePriceStrategy: có thể thay thế hoàn toàn StandardBidValidation (LSP)")
    void testReservePriceStrategyIsSubstitutable() {
        // Dùng interface — client code không cần biết implementation cụ thể
        BidValidationStrategy strategy = new ReservePriceBidValidation(0.0); // reserve = 0 → không chặn

        Auction auction = new Auction("i3", "s3", "LSP Test", 1000.0,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1));
        auction.start();

        assertDoesNotThrow(() -> strategy.validate(auction, "bidder-1", 1500.0));
    }

    @Test
    @DisplayName("ReservePriceStrategy: constructor từ chối reserve âm")
    void testReservePriceNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                new ReservePriceBidValidation(-100.0));
    }
}

