package com.auction.model.auction;


import com.auction.model.transaction.BidTransaction;
import com.auction.model.entity.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Lớp Auction - quản lý phiên đấu giá.
 * Sử dụng ReentrantLock để xử lý concurrent bidding.
 * Hỗ trợ anti-sniping algorithm.
 */
public class Auction extends Entity {

    private static final long serialVersionUID = 1L;

    /** Thời gian gia hạn anti-sniping (giây). */
    private static final int ANTI_SNIPE_THRESHOLD_SECONDS = 30;
    private static final int ANTI_SNIPE_EXTENSION_SECONDS = 60;

    private String itemId;
    private String sellerId;
    private String itemName;
    private double startingPrice;
    private double currentHighestBid;
    private String currentHighestBidderId;
    private String currentHighestBidderName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private final List<BidTransaction> bidHistory;
    private int totalBids;

    // Anti-sniping
    private boolean antiSnipingEnabled;
    private int snipeExtensionCount;

    // Concurrency lock - transient vì không serialize được
    private transient ReentrantLock bidLock;

    // Auto-bidding
    private final List<AutoBidConfig> autoBids;

    public Auction() {
        super();
        this.bidHistory = new ArrayList<>();
        this.autoBids = new ArrayList<>();
        this.status = AuctionStatus.OPEN;
        this.bidLock = new ReentrantLock(true); // fair lock
        this.antiSnipingEnabled = true;
        this.totalBids = 0;
        this.snipeExtensionCount = 0;
    }

    public Auction(String itemId, String sellerId, String itemName,
                   double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super();
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.itemName = itemName;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new ArrayList<>();
        this.autoBids = new ArrayList<>();
        this.bidLock = new ReentrantLock(true);
        this.antiSnipingEnabled = true;
        this.totalBids = 0;
        this.snipeExtensionCount = 0;
    }

    /**
     * Đảm bảo lock được khởi tạo sau deserialization.
     */
    public ReentrantLock getBidLock() {
        if (bidLock == null) {
            bidLock = new ReentrantLock(true);
        }
        return bidLock;
    }

    /**
     * Đặt giá đấu - thread-safe với ReentrantLock.
     * Xử lý concurrent bidding, anti-sniping.
     *
     * @return BidTransaction nếu hợp lệ, null nếu không
     */
    public BidTransaction placeBid(String bidderId, String bidderName, double amount) {
        getBidLock().lock();
        try {
            // Kiểm tra trạng thái phiên
            if (status != AuctionStatus.RUNNING) {
                return null;
            }

            // Kiểm tra thời gian
            if (LocalDateTime.now().isAfter(endTime)) {
                return null;
            }

            // Kiểm tra giá hợp lệ
            if (amount <= currentHighestBid) {
                return null;
            }

            // Không cho phép seller tự đấu giá
            if (bidderId.equals(sellerId)) {
                return null;
            }

            // Tạo giao dịch mới
            BidTransaction transaction = new BidTransaction(
                    getId(), bidderId, bidderName, amount, currentHighestBid);

            // Cập nhật thông tin
            this.currentHighestBid = amount;
            this.currentHighestBidderId = bidderId;
            this.currentHighestBidderName = bidderName;
            this.totalBids++;
            this.bidHistory.add(transaction);

            // Anti-sniping: gia hạn nếu bid trong X giây cuối
            if (antiSnipingEnabled) {
                checkAndExtendForAntiSniping();
            }

            markUpdated();
            return transaction;
        } finally {
            getBidLock().unlock();
        }
    }

    /**
     * Anti-sniping algorithm.
     * Nếu bid mới trong ANTI_SNIPE_THRESHOLD_SECONDS giây cuối,
     * tự động gia hạn thêm ANTI_SNIPE_EXTENSION_SECONDS giây.
     */
    private void checkAndExtendForAntiSniping() {
        LocalDateTime now = LocalDateTime.now();
        long secondsRemaining = java.time.Duration.between(now, endTime).getSeconds();

        if (secondsRemaining <= ANTI_SNIPE_THRESHOLD_SECONDS && secondsRemaining > 0) {
            this.endTime = endTime.plusSeconds(ANTI_SNIPE_EXTENSION_SECONDS);
            this.snipeExtensionCount++;
        }
    }

    /**
     * Bắt đầu phiên đấu giá.
     */
    public void start() {
        if (status == AuctionStatus.OPEN) {
            this.status = AuctionStatus.RUNNING;
            markUpdated();
        }
    }

    /**
     * Kết thúc phiên đấu giá, xác định người thắng.
     */
    public void finish() {
        getBidLock().lock();
        try {
            if (status == AuctionStatus.RUNNING) {
                this.status = AuctionStatus.FINISHED;
                markUpdated();
            }
        } finally {
            getBidLock().unlock();
        }
    }

    /**
     * Đánh dấu đã thanh toán.
     */
    public void markPaid() {
        if (status == AuctionStatus.FINISHED) {
            this.status = AuctionStatus.PAID;
            markUpdated();
        }
    }

    /**
     * Hủy phiên đấu giá.
     * Giữ lock để tránh race condition với placeBid() đang chạy đồng thời.
     */
    public void cancel() {
        getBidLock().lock();
        try {
            this.status = AuctionStatus.CANCELED;
            markUpdated();
        } finally {
            getBidLock().unlock();
        }
    }

    /**
     * Kiểm tra phiên đã hết thời gian chưa.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }

    /**
     * Kiểm tra phiên đang hoạt động.
     */
    public boolean isActive() {
        return status == AuctionStatus.RUNNING && !isExpired();
    }

    /**
     * Thêm auto-bid config.
     */
    public void addAutoBid(AutoBidConfig config) {
        getBidLock().lock();
        try {
            // Xóa auto-bid cũ của cùng bidder
            autoBids.removeIf(ab -> ab.getBidderId().equals(config.getBidderId()));
            autoBids.add(config);
        } finally {
            getBidLock().unlock();
        }
    }

    /**
     * Xử lý auto-bidding sau khi có bid mới.
     *
     * <p>Vòng lặp tiếp tục cho đến khi người dẫn đầu hiện tại không còn ai
     * có thể vượt qua (stable leader).  Mỗi lần một bidder đặt giá thành
     * công, vòng lặp restart từ đầu danh sách (ưu tiên đăng ký sớm hơn)
     * để cho tất cả auto-bidder có cơ hội phản ứng – mô phỏng đúng cơ chế
     * đấu giá tự động thực tế.
     *
     * <p>Ghi chú: phương thức thực hiện trực tiếp bên trong lock để tránh
     * deadlock khi gọi {@link #placeBid} (cũng lock).
     *
     * @param excludeBidderId ID của người vừa đặt giá thủ công (không để
     *                        auto-bid ngay lập tức phản lại chính họ).
     *                        Truyền chuỗi rỗng {@code ""} nếu không cần loại trừ ai.
     * @return danh sách các BidTransaction được tạo bởi auto-bid (theo thứ tự)
     */
    public List<BidTransaction> processAutoBids(String excludeBidderId) {
        List<BidTransaction> autoBidTransactions = new ArrayList<>();
        getBidLock().lock();
        try {
            boolean anyBidPlaced = true;
            while (anyBidPlaced) {
                anyBidPlaced = false;

                // Ưu tiên auto-bidder đăng ký trước (FIFO tiebreaker)
                List<AutoBidConfig> sorted = new ArrayList<>(autoBids);
                sorted.sort((a, b) -> a.getRegisteredAt().compareTo(b.getRegisteredAt()));

                for (AutoBidConfig autoBid : sorted) {
                    // Bỏ qua người vừa trigger (tránh phản lại chính mình ngay lập tức)
                    if (autoBid.getBidderId().equals(excludeBidderId)) continue;
                    // Bỏ qua nếu đã đang dẫn đầu
                    if (autoBid.getBidderId().equals(currentHighestBidderId)) continue;

                    double newBid = currentHighestBid + autoBid.getIncrement();
                    if (newBid > autoBid.getMaxBid()) continue; // Vượt giới hạn tối đa

                    // Đặt giá auto-bid trực tiếp (bên trong lock, tránh deadlock)
                    BidTransaction tx = new BidTransaction(
                            getId(), autoBid.getBidderId(), autoBid.getBidderName(),
                            newBid, currentHighestBid);

                    this.currentHighestBid = newBid;
                    this.currentHighestBidderId = autoBid.getBidderId();
                    this.currentHighestBidderName = autoBid.getBidderName();
                    this.totalBids++;
                    this.bidHistory.add(tx);
                    autoBidTransactions.add(tx);

                    if (antiSnipingEnabled) {
                        checkAndExtendForAntiSniping();
                    }

                    // Một bid thành công → restart vòng lặp ngoài để tất cả
                    // auto-bidder có cơ hội phản ứng với người dẫn đầu mới
                    anyBidPlaced = true;
                    break;
                }
            }

            if (!autoBidTransactions.isEmpty()) {
                markUpdated();
            }
        } finally {
            getBidLock().unlock();
        }
        return autoBidTransactions;
    }

    // Getters & Setters
    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public String getCurrentHighestBidderId() {
        return currentHighestBidderId;
    }

    public String getCurrentHighestBidderName() {
        return currentHighestBidderName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public List<BidTransaction> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }

    public int getTotalBids() {
        return totalBids;
    }

    public boolean isAntiSnipingEnabled() {
        return antiSnipingEnabled;
    }

    public void setAntiSnipingEnabled(boolean antiSnipingEnabled) {
        this.antiSnipingEnabled = antiSnipingEnabled;
    }

    public int getSnipeExtensionCount() {
        return snipeExtensionCount;
    }

    public List<AutoBidConfig> getAutoBids() {
        return Collections.unmodifiableList(autoBids);
    }

    @Override
    public String printInfo() {
        return String.format(
                "Auction[id=%s, item=%s, status=%s, currentBid=%.2f, bids=%d, endTime=%s]",
                getId(), itemName, status, currentHighestBid, totalBids, endTime);
    }
}