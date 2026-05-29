package com.auction.service;

import com.auction.dao.AuctionDaoImpl;
import com.auction.dao.GenericDao;
import com.auction.dao.ItemDaoImpl;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.auction.AutoBidConfig;
import com.auction.model.item.Item;
import com.auction.model.item.ItemCategory;
import com.auction.model.transaction.BidTransaction;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.pattern.factory.ItemFactory;
import com.auction.pattern.observer.AuctionEvent;
import com.auction.pattern.observer.AuctionEventDispatcher;
import com.auction.pattern.singleton.AuctionManager;
import com.auction.pattern.strategy.BidValidationStrategy;
import com.auction.pattern.strategy.StandardBidValidation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Service xử lý nghiệp vụ đấu giá — lớp orchestration trung tâm cho toàn bộ
 * vòng đời phiên đấu giá (tạo, đặt giá, auto-bid, kết thúc, thanh toán, huỷ).
 *
 * <p><b>Vai trò:</b> đóng vai trò facade giữa tầng UI/controller và tầng
 * domain/DAO. Service không tự cache state mà uỷ thác cho {@link AuctionManager}
 * (singleton in-memory) và DAO ({@link AuctionDaoImpl}).
 *
 * <p><b>Tích hợp design patterns:</b>
 * <ul>
 *   <li><b>Factory</b> — {@link ItemFactory} tạo Item theo {@link ItemCategory}</li>
 *   <li><b>Strategy</b> — {@link BidValidationStrategy} cho luật validate bid</li>
 *   <li><b>Observer</b> — {@link AuctionEventDispatcher} broadcast các sự kiện
 *       NEW_BID / AUTO_BID / AUCTION_STARTED / AUCTION_ENDED / AUCTION_CANCELED</li>
 *   <li><b>Singleton</b> — {@link AuctionManager}, {@link AuctionEventDispatcher}</li>
 * </ul>
 *
 * <p><b>Thanh toán (settlement):</b> Khi {@link #endAuction(String)} được gọi,
 * {@link #settleAuction(Auction)} sẽ:
 * <ol>
 *   <li>Trừ số dư ({@link Bidder#deductBalance}) của người thắng.</li>
 *   <li>Cộng doanh thu ({@link Seller#addRevenue}) cho người bán.</li>
 * </ol>
 * Nếu số dư người thắng không đủ, phiên vẫn kết thúc nhưng ghi log cảnh báo;
 * trong hệ thống thực cần cơ chế escrow/payment gateway để giữ tiền trước
 * khi cho phép bid.
 *
 * <p><b>Thread-safety:</b> Bản thân service không chứa state mutable; an toàn
 * khi nhiều thread cùng gọi. Việc đồng bộ thực sự diễn ra ở lớp {@link Auction}
 * (dùng {@code ReentrantLock} nội bộ) và ở SQLite (file-level lock).
 */
public class AuctionService {

    private final GenericDao<Item>    itemDao;
    private final GenericDao<Auction> auctionDao;
    private final UserService         userService;
    private final AuctionManager      auctionManager;
    private final AuctionEventDispatcher eventDispatcher;
    private final BidValidationStrategy  bidValidator;

    /**
     * BẢN ĐỒ Ổ KHÓA (LOCK MAP): Quản lý các ổ khóa độc lập theo từng Auction ID.
     * Đảm bảo tính duy nhất của Object khóa trên RAM cho mỗi phòng đấu giá.
     * Giúp hệ thống không bị nghẽn cổ chai toàn cục (Global Bottleneck).
     */
    private final Cache<String, ReentrantLock> lockCache = CacheBuilder.newBuilder()
        .weakValues() // ĐÂY LÀ CHÌA KHÓA: Tự dọn dẹp lock khi không dùng đến nữa
        .build();

    // Hàm tiện ích để lấy Lock theo Auction ID
    private ReentrantLock getLock(String auctionId) {
        try {
            // Nếu có lock cũ thì trả về, nếu chưa có thì chạy hàm ReentrantLock::new để tạo mới
            return lockCache.get(auctionId, ReentrantLock::new);
        } catch (ExecutionException e) {
            // Phòng hờ lỗi khởi tạo, trả về một lock mới
            return new ReentrantLock();
        }
    }

    /** Constructor mặc định (production). */
    public AuctionService() {
        this.itemDao        = new ItemDaoImpl();
        this.auctionDao     = new AuctionDaoImpl();
        this.userService    = new UserService();
        this.auctionManager = AuctionManager.getInstance();
        this.eventDispatcher = AuctionEventDispatcher.getInstance();
        this.bidValidator   = new StandardBidValidation();
    }

    /**
     * Constructor injection cho unit tests (2 DAOs, tự tạo UserService).
     * Khi UserService sử dụng DAO mặc định (file-based) và người dùng không
     * tồn tại trong file, settlement sẽ silently no-op — hành vi an toàn cho tests.
     */
    public AuctionService(GenericDao<Item> itemDao, GenericDao<Auction> auctionDao) {
        this(itemDao, auctionDao, new UserService());
    }

    /**
     * Constructor injection đầy đủ (dùng trong tests yêu cầu settlement kiểm chứng được).
     */
    public AuctionService(GenericDao<Item> itemDao, GenericDao<Auction> auctionDao,
                          UserService userService) {
        this.itemDao         = itemDao;
        this.auctionDao      = auctionDao;
        this.userService     = userService;
        this.auctionManager  = AuctionManager.getInstance();
        this.eventDispatcher = AuctionEventDispatcher.getInstance();
        this.bidValidator    = new StandardBidValidation();
    }

    // ==================== Item Management ====================

    /**
     * Thêm sản phẩm mới - sử dụng Factory Method.
     */
    public Item createItem(ItemCategory category, String name, String description,
                           double startingPrice, String sellerId, Map<String, String> extra) {
        Item item = ItemFactory.createItem(category, name, description, startingPrice, sellerId, extra);
        itemDao.save(item);
        return item;
    }

    public Optional<Item> getItem(String itemId)   { return itemDao.findById(itemId); }
    public List<Item>     getAllItems()            { return itemDao.findAll(); }
    public void           updateItem(Item item)    { itemDao.update(item); }
    public void           deleteItem(String itemId){ itemDao.delete(itemId); }

    // ==================== Auction Management ====================

    /**
     * Tạo phiên đấu giá mới.
     */
    public Auction createAuction(String itemId, String sellerId, String itemName,
                                 double startingPrice, LocalDateTime startTime,
                                 LocalDateTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }
        if (startingPrice <= 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0");
        }

        Auction auction = new Auction(itemId, sellerId, itemName, startingPrice, startTime, endTime);

        // Tự động bắt đầu nếu đã quá thời gian bắt đầu
        if (LocalDateTime.now().isAfter(startTime)) {
            auction.start();
        }

        // 1. CÀI BÁO THỨC MỞ PHÒNG bằng Lambda
        long startDelay = java.time.Duration.between(LocalDateTime.now(), startTime).toSeconds();
        if (startDelay > 0) {
            // Ta bảo Manager: "Đến giờ thì tự chạy hàm startAuctionProactively(auctionId) của tôi nhé"
            auctionManager.scheduleAuctionStart(startDelay, () -> this.startAuctionProactively(auction.getId()));
        } else {
            auction.start();
            auctionDao.update(auction);
        }

        // 2. CÀI BÁO THỨC ĐÓNG PHÒNG bằng Lambda
        long endDelay = java.time.Duration.between(LocalDateTime.now(), endTime).toSeconds();
        if (endDelay > 0) {
            // Ta bảo Manager: "Đến giờ thì tự gọi hàm endAuction(auctionId) của tôi"
            auctionManager.scheduleAuctionEnd(endDelay, () -> this.endAuction(auction.getId()));
        }

        auctionDao.save(auction);
        auctionManager.addAuction(auction);

        eventDispatcher.dispatch(new AuctionEvent(
                AuctionEvent.EventType.AUCTION_STARTED,
                auction.getId(),
                "Phiên đấu giá '" + itemName + "' đã được tạo"));

        return auction;
    }

    public void startAuctionProactively(String auctionId) {
        ReentrantLock lock = getLock(auctionId);
        boolean isStarted = false;

        lock.lock(); // Thay thế synchronized bằng ReentrantLock từ Guava Cache
        try {
            Auction auction = resolveAuction(auctionId);
            if (auction != null && auction.getStatus() == AuctionStatus.OPEN) {
                auction.start();
                auctionDao.update(auction);
                isStarted = true;
            }
        } finally {
            lock.unlock(); // Luôn luôn giải phóng khóa trong khối finally
        }

        // Bắn event ra ngoài để đẩy qua WebSocket lên GUI realtime!
        if (isStarted) {
            eventDispatcher.dispatch(new AuctionEvent(
                AuctionEvent.EventType.AUCTION_STARTED,
                auctionId,
                "Phiên đấu giá đã chính thức bắt đầu! Đặt giá ngay!"));
        }
    }

    /**
     * Đặt giá đấu - xử lý concurrent bidding.
     */
    public BidTransaction placeBid(String auctionId, String bidderId, String bidderName,
                                   double amount) throws InvalidBidException, AuctionClosedException {
        ReentrantLock lock = getLock(auctionId);

        Auction auction;
        BidTransaction transaction;
        List<BidTransaction> autoBidResults;

        lock.lock(); // Đảm bảo tại một thời điểm chỉ có 1 thread được đặt cược vào phòng này
        try {
            auction = resolveAuction(auctionId);
            if (auction == null) {
                throw new InvalidBidException("Không tìm thấy phiên đấu giá: " + auctionId);
            }
            // Validate bằng Strategy Pattern
            try {
                bidValidator.validate(auction, bidderId, amount);
            } catch (IllegalArgumentException e) {
                if (auction.getStatus() != AuctionStatus.RUNNING || auction.isExpired()) {
                    throw new AuctionClosedException(e.getMessage());
                }
                throw new InvalidBidException(e.getMessage());
            }

            // Đặt giá - thread-safe (Auction nội bộ dùng ReentrantLock)
            transaction = auction.placeBid(bidderId, bidderName, amount);
            if (transaction == null) {
                throw new InvalidBidException("Không thể đặt giá. Vui lòng thử lại.");
            }

            // Xử lý auto-bidding.
            // Tối ưu: chỉ persist xuống DAO 1 lần sau khi cả burst kết thúc
            autoBidResults = auction.processAutoBids(bidderId);
            auctionDao.update(auction);
        } finally {
            lock.unlock();
        }

        // Thông báo qua Observer Pattern
        eventDispatcher.dispatch(new AuctionEvent(
                AuctionEvent.EventType.NEW_BID,
                auctionId, transaction,
                String.format("%s đã đặt giá %.2f", bidderName, amount)));

        if (!autoBidResults.isEmpty()) {
            for (BidTransaction autoBid : autoBidResults) {
                eventDispatcher.dispatch(new AuctionEvent(
                        AuctionEvent.EventType.AUTO_BID,
                        auctionId, autoBid,
                        String.format("[Auto] %s đã tự động đặt giá %.2f",
                                autoBid.getBidderName(), autoBid.getBidAmount())));
            }
        }
        return transaction;
    }

    /**
     * Đăng ký auto-bidding.
     */
    public void registerAutoBid(String auctionId, String bidderId, String bidderName,
                                double maxBid, double increment) throws InvalidBidException {
        ReentrantLock lock = getLock(auctionId);

        Auction auction;
        List<BidTransaction> autoBidResults = List.of();

        lock.lock();
        try {
            auction = resolveAuction(auctionId);
            if (auction == null) {
                throw new InvalidBidException("Không tìm thấy phiên đấu giá");
            }
            if (maxBid <= auction.getCurrentHighestBid()) {
                throw new InvalidBidException("Giá tối đa phải cao hơn giá hiện tại");
            }
            if (increment <= 0) {
                throw new InvalidBidException("Bước giá phải lớn hơn 0");
            }

            // Thay đổi cấu hình AutoBid trên RAM
            AutoBidConfig config = new AutoBidConfig(bidderId, bidderName, maxBid, increment);
            auction.addAutoBid(config);

            // Ngay lập tức xử lý auto-bid nếu chưa dẫn đầu.
            // excludeBidderId = "" → không loại trừ ai (người mới đăng ký cũng được phép bid ngay).
            if (!bidderId.equals(auction.getCurrentHighestBidderId())) {
                autoBidResults = auction.processAutoBids("");
            }

            // Tối ưu: chỉ persist 1 lần sau cả burst.
            auctionDao.update(auction);
        } finally {
            lock.unlock();
        }

        if (!autoBidResults.isEmpty()) {
            for (BidTransaction tx : autoBidResults) {
                eventDispatcher.dispatch(new AuctionEvent(
                    AuctionEvent.EventType.AUTO_BID,
                    auctionId, tx,
                    String.format("[Auto] %s đã tự động đặt giá %.2f",
                        tx.getBidderName(), tx.getBidAmount())));
            }
        }
    }

    /**
     * Lấy Auction theo ID — cache-first với DB fallback.
     *
     * <p>Thử lấy từ AuctionManager (in-memory cache) trước. Nếu cache miss
     * (server vừa restart, phiên chưa được load, hoặc phiên đã bị evict),
     * tự động fallback sang DB và warm cache để lần sau không miss nữa.
     *
     * <p>Đây là pattern <b>Cache-Aside (Lazy Loading)</b>: chỉ load vào cache
     * khi thực sự cần, thay vì preload toàn bộ.
     */
    public Optional<Auction> getAuction(String auctionId) {
        Auction cached = auctionManager.getAuction(auctionId);
        if (cached != null) return Optional.of(cached);
        // Cache miss → fallback DB và warm cache
        return getFreshAuction(auctionId);
    }

    /**
     * Lấy Auction trực tiếp từ DB (bỏ qua cache) — dùng khi cần data mới nhất
     * (vd: client vừa mở trang chi tiết).
     */
    public Optional<Auction> getFreshAuction(String auctionId) {
        Optional<Auction> opt = auctionDao.findById(auctionId);
        opt.ifPresent(auctionManager::addAuction); // warm cache
        return opt;
    }

    /**
     * Lấy tất cả phiên đấu giá — DB là nguồn chính (vì cache chỉ giữ phiên active),
     * nhưng ưu tiên object từ cache cho các phiên đang OPEN/RUNNING.
     *
     * <p><b>Tại sao ưu tiên cache?</b> Với phiên đang chạy, bản cache có state
     * mới nhất (bid in-memory chưa flush xuống DB), trong khi bản DB có thể stale
     * vài giây. Phiên đã kết thúc/huỷ thì lấy từ DB vì cache đã evict.
     */
    public List<Auction> getAllAuctions() {
        List<Auction> fromDb = auctionDao.findAll();
        List<Auction> result = new ArrayList<>(fromDb.size());
        for (Auction dbAuction : fromDb) {
            Auction cached = auctionManager.getAuction(dbAuction.getId());
            if (cached != null) {
                // Bản cache mới hơn cho phiên active → ưu tiên
                result.add(cached);
            } else {
                result.add(dbAuction);
                // Warm cache nếu phiên đang active mà chưa có trong cache
                if (dbAuction.getStatus() == AuctionStatus.OPEN
                        || dbAuction.getStatus() == AuctionStatus.RUNNING) {
                    auctionManager.addAuction(dbAuction);
                }
            }
        }
        return result;
    }

    /**
     * Lấy các phiên đang chạy — cache-first, warm từ DB nếu cache rỗng.
     *
     * <p>Trường hợp cache rỗng xảy ra khi server vừa restart — chưa có client
     * nào trigger load. Method này tự warm cache từ DB một lần, các lần gọi
     * tiếp theo sẽ hit cache trực tiếp (O(n) filter trên ConcurrentHashMap).
     */
    public List<Auction> getActiveAuctions() {
        List<Auction> cached = auctionManager.getActiveAuctions();
        if (!cached.isEmpty()) return cached;
        // Cache cold → warm từ DB
        warmCacheFromDb();
        return auctionManager.getActiveAuctions();
    }

    /**
     * Lấy các phiên của 1 seller — cache-first, warm từ DB nếu cache rỗng.
     */
    public List<Auction> getAuctionsBySeller(String sellerId) {
        List<Auction> cached = auctionManager.getAuctionsBySeller(sellerId);
        if (!cached.isEmpty()) return cached;
        // Cache cold → warm từ DB rồi thử lại
        warmCacheFromDb();
        return auctionManager.getAuctionsBySeller(sellerId);
    }

    /**
     * Nạp tất cả phiên OPEN/RUNNING từ DB vào cache.
     * Gọi khi phát hiện cache rỗng (server vừa restart).
     */
    private void warmCacheFromDb() {
        for (Auction a : auctionDao.findAll()) {
            if (a.getStatus() == AuctionStatus.OPEN || a.getStatus() == AuctionStatus.RUNNING) {
                auctionManager.addAuction(a);
            }
        }
    }

    /**
     * Kết thúc phiên đấu giá thủ công và thực hiện thanh toán tự động.
     *
     * <p>Thứ tự: finish() → settleAuction() → dispatch AUCTION_ENDED event.
     */
    public void endAuction(String auctionId) {
        ReentrantLock lock = getLock(auctionId);
        Auction auction;

        lock.lock();
        try {
            auction = resolveAuction(auctionId);
            if (auction == null) return;

            auction.finish();
            auctionDao.update(auction);
        } finally {
            lock.unlock();
        }

        // Thực hiện chuyển tiền (trừ người thắng, cộng người bán)
        settleAuction(auction);

        eventDispatcher.dispatch(new AuctionEvent(
            AuctionEvent.EventType.AUCTION_ENDED,
            auctionId,
            "Phiên đấu giá đã kết thúc. Người thắng: "
                + (auction.getCurrentHighestBidderName() != null
                ? auction.getCurrentHighestBidderName() : "Không có")));
    }

    /**
     * Thực hiện thanh toán sau khi phiên kết thúc.
     *
     * <ul>
     *   <li>Trừ {@link Bidder#deductBalance} của người thắng.</li>
     *   <li>Cộng {@link Seller#addRevenue} cho người bán.</li>
     * </ul>
     *
     * <p>Nếu người thắng không có đủ số dư, ghi cảnh báo nhưng không rollback
     * (trong môi trường thực cần escrow trước khi cho phép bid).
     * Nếu người dùng không tìm thấy trong hệ thống, settlement bị bỏ qua an toàn.
     */
    private void settleAuction(Auction auction) {
        String winnerId = auction.getCurrentHighestBidderId();
        if (winnerId == null) return; // Không có ai bid

        double saleAmount = auction.getCurrentHighestBid();

        // --- Trừ tiền người thắng ---
        userService.findById(winnerId).ifPresent(winnerUser -> {
            if (winnerUser instanceof Bidder bidder) {
                boolean deducted = bidder.deductBalance(saleAmount);
                if (!deducted) {
                    System.err.printf("[Settlement] Cảnh báo: Bidder %s không đủ số dư (%.2f) " +
                            "cho phiên %s%n", bidder.getUsername(), saleAmount, auction.getId());
                    // Vẫn ghi nhận giao dịch; trong hệ thống thực cần xử lý thêm
                }
                userService.updateUser(bidder);
            }
        });

        // --- Cộng doanh thu người bán ---
        userService.findById(auction.getSellerId()).ifPresent(sellerUser -> {
            if (sellerUser instanceof Seller seller) {
                seller.addRevenue(saleAmount);
                userService.updateUser(seller);
            }
        });
    }

    /**
     * Hủy phiên đấu giá.
     */
    public void cancelAuction(String auctionId) {
        ReentrantLock lock = getLock(auctionId);
        boolean isCanceledSuccessfully = false;

        lock.lock();
        try {
            Auction auction = resolveAuction(auctionId);
            if (auction != null) {
                auction.cancel();
                auctionDao.update(auction);
                isCanceledSuccessfully = true;
                eventDispatcher.dispatch(new AuctionEvent(
                    AuctionEvent.EventType.AUCTION_CANCELED,
                    auctionId,
                    "Phiên đấu giá đã bị hủy"));
            }
        } finally {
            lock.unlock();
        }
        if (isCanceledSuccessfully) {
            eventDispatcher.dispatch(new AuctionEvent(
                AuctionEvent.EventType.AUCTION_CANCELED,
                auctionId,
                "Phiên đấu giá đã bị hủy"));
        }
    }

    /**
     * Lấy lịch sử bid của phiên — cache-first với DB fallback.
     */
    public List<BidTransaction> getBidHistory(String auctionId) {
        Auction auction = resolveAuction(auctionId);
        return auction != null ? auction.getBidHistory() : List.of();
    }

    // ==================== Internal Helpers ====================

    /**
     * Lấy Auction từ cache, fallback sang DB nếu cache miss.
     *
     * <p>Dùng nội bộ bởi các method nghiệp vụ (placeBid, endAuction...)
     * thay vì truy cập trực tiếp {@code auctionManager.getAuction()} —
     * đảm bảo mọi thao tác đều xử lý được cache miss (vd: server vừa
     * restart mà client gọi placeBid ngay trước khi cache được warm).
     *
     * @return Auction object hoặc null nếu không tìm thấy cả trong cache lẫn DB
     */
    private Auction resolveAuction(String auctionId) {
        Auction auction = auctionManager.getAuction(auctionId);
        if (auction != null) return auction;
        // Cache miss → load từ DB và warm cache
        Optional<Auction> opt = auctionDao.findById(auctionId);
        opt.ifPresent(auctionManager::addAuction);
        return opt.orElse(null);
    }
}
