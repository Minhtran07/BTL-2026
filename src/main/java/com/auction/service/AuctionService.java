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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
    private final ConcurrentHashMap<String, Object> auctionLocks = new ConcurrentHashMap<>();

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
    public List<Item>     getAllItems()             { return itemDao.findAll(); }
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

        auctionDao.save(auction);
        auctionManager.addAuction(auction);

        eventDispatcher.dispatch(new AuctionEvent(
                AuctionEvent.EventType.AUCTION_STARTED,
                auction.getId(),
                "Phiên đấu giá '" + itemName + "' đã được tạo"));

        return auction;
    }

    /**
     * Đặt giá đấu - xử lý concurrent bidding.
     */
    public BidTransaction placeBid(String auctionId, String bidderId, String bidderName,
                                   double amount) throws InvalidBidException, AuctionClosedException {
        Object lock = auctionLocks.computeIfAbsent(auctionId, k -> new Object());
        synchronized (lock) {
            Auction auction = auctionManager.getAuction(auctionId);
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
            BidTransaction transaction = auction.placeBid(bidderId, bidderName, amount);
            if (transaction == null) {
                throw new InvalidBidException("Không thể đặt giá. Vui lòng thử lại.");
            }

            auctionDao.update(auction);

            // Thông báo qua Observer Pattern
            eventDispatcher.dispatch(new AuctionEvent(
                    AuctionEvent.EventType.NEW_BID,
                    auctionId, transaction,
                    String.format("%s đã đặt giá %.2f", bidderName, amount)));

            // Xử lý auto-bidding.
            // Tối ưu: chỉ persist xuống DAO 1 lần sau khi cả burst kết thúc
            // (processAutoBids đã giữ lock và sinh hết transaction trước khi return).
            List<BidTransaction> autoBidResults = auction.processAutoBids(bidderId);
            if (!autoBidResults.isEmpty()) {
                auctionDao.update(auction);
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
    }

    /**
     * Đăng ký auto-bidding.
     */
    public void registerAutoBid(String auctionId, String bidderId, String bidderName,
                                double maxBid, double increment) throws InvalidBidException {
        Object lock = auctionLocks.computeIfAbsent(auctionId, k -> new Object());
        synchronized (lock) {
            Auction auction = auctionManager.getAuction(auctionId);
            if (auction == null) {
                throw new InvalidBidException("Không tìm thấy phiên đấu giá");
            }
            if (maxBid <= auction.getCurrentHighestBid()) {
                throw new InvalidBidException("Giá tối đa phải cao hơn giá hiện tại");
            }
            if (increment <= 0) {
                throw new InvalidBidException("Bước giá phải lớn hơn 0");
            }

            AutoBidConfig config = new AutoBidConfig(bidderId, bidderName, maxBid, increment);
            auction.addAutoBid(config);
            auctionDao.update(auction);

            // Ngay lập tức xử lý auto-bid nếu chưa dẫn đầu.
            // excludeBidderId = "" → không loại trừ ai (người mới đăng ký cũng được phép bid ngay).
            // Tối ưu: chỉ persist 1 lần sau cả burst.
            if (!bidderId.equals(auction.getCurrentHighestBidderId())) {
                List<BidTransaction> results = auction.processAutoBids("");
                if (!results.isEmpty()) {
                    auctionDao.update(auction);
                    for (BidTransaction tx : results) {
                        eventDispatcher.dispatch(new AuctionEvent(
                            AuctionEvent.EventType.AUTO_BID,
                            auctionId, tx,
                            String.format("[Auto] %s đã tự động đặt giá %.2f",
                                tx.getBidderName(), tx.getBidAmount())));
                    }
                }
            }
        }
    }

    public Optional<Auction> getAuction(String auctionId) {
        return Optional.ofNullable(auctionManager.getAuction(auctionId));
    }

    public List<Auction> getAllAuctions()                     { return auctionManager.getAllAuctions(); }
    public List<Auction> getActiveAuctions()                  { return auctionManager.getActiveAuctions(); }
    public List<Auction> getAuctionsBySeller(String sellerId) { return auctionManager.getAuctionsBySeller(sellerId); }

    /**
     * Kết thúc phiên đấu giá thủ công và thực hiện thanh toán tự động.
     *
     * <p>Thứ tự: finish() → settleAuction() → dispatch AUCTION_ENDED event.
     */
    public void endAuction(String auctionId) {
        Object lock = auctionLocks.computeIfAbsent(auctionId, k -> new Object());
        synchronized (lock) {
            Auction auction = auctionManager.getAuction(auctionId);
            if (auction == null) return;

            auction.finish();
            auctionDao.update(auction);

            // Thực hiện chuyển tiền (trừ người thắng, cộng người bán)
            settleAuction(auction);

            eventDispatcher.dispatch(new AuctionEvent(
                AuctionEvent.EventType.AUCTION_ENDED,
                auctionId,
                "Phiên đấu giá đã kết thúc. Người thắng: "
                    + (auction.getCurrentHighestBidderName() != null
                    ? auction.getCurrentHighestBidderName() : "Không có")));
        }
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
        Object lock = auctionLocks.computeIfAbsent(auctionId, k -> new Object());
        synchronized (lock) {
            Auction auction = auctionManager.getAuction(auctionId);
            if (auction != null) {
                auction.cancel();
                auctionDao.update(auction);
                eventDispatcher.dispatch(new AuctionEvent(
                    AuctionEvent.EventType.AUCTION_CANCELED,
                    auctionId,
                    "Phiên đấu giá đã bị hủy"));
            }
        }
    }

    /**
     * Lấy lịch sử bid của phiên.
     */
    public List<BidTransaction> getBidHistory(String auctionId) {
        Auction auction = auctionManager.getAuction(auctionId);
        return auction != null ? auction.getBidHistory() : List.of();
    }
}
