package com.auction.pattern.singleton.observer;

import com.auction.model.transaction.BidTransaction;

/**
 * ============================================================================
 * LỚP AUCTIONEVENT - "VẬT MANG" THÔNG TIN SỰ KIỆN ĐẤU GIÁ
 * ============================================================================
 *
 * <p>Đây là data class trong Observer Pattern - chứa thông tin về 1 sự kiện
 * xảy ra trong phiên đấu giá. Khi dispatcher gọi
 * {@link AuctionObserver#onAuctionEvent(AuctionEvent)}, nó truyền object này
 * vào để observer biết "sự kiện gì, ở phiên nào, dữ liệu liên quan thế nào".
 *
 * <p><b>IMMUTABLE:</b> Tất cả field {@code final} - sau khi tạo, không ai có
 * thể sửa. Tại sao? An toàn trong multi-thread (không lock), tránh observer
 * này thấy event đã được observer kia "biến đổi".
 */
public class AuctionEvent {

    /**
     * Enum liệt kê các loại sự kiện có thể xảy ra trong phiên đấu giá.
     *
     * <ul>
     *   <li>{@code NEW_BID}: Có người vừa đặt giá mới (thủ công)</li>
     *   <li>{@code AUCTION_STARTED}: Phiên vừa bắt đầu (OPEN → RUNNING)</li>
     *   <li>{@code AUCTION_ENDED}: Phiên vừa kết thúc (RUNNING → FINISHED)</li>
     *   <li>{@code AUCTION_EXTENDED}: Phiên được gia hạn (anti-sniping)</li>
     *   <li>{@code AUTO_BID}: Có auto-bid được kích hoạt</li>
     *   <li>{@code AUCTION_CANCELED}: Phiên bị seller hủy</li>
     * </ul>
     *
     * Observer có thể dùng switch trên type để xử lý từng loại sự kiện khác nhau.
     */
    public enum EventType {
        NEW_BID,
        AUCTION_STARTED,
        AUCTION_ENDED,
        AUCTION_EXTENDED,
        AUTO_BID,
        AUCTION_CANCELED
    }

    /** Loại sự kiện. */
    private final EventType type;

    /** ID phiên đấu giá liên quan. */
    private final String auctionId;

    /**
     * Giao dịch bid liên quan (nếu là event NEW_BID hoặc AUTO_BID).
     * Có thể null với các event không liên quan đến bid (vd: AUCTION_STARTED).
     */
    private final BidTransaction transaction;

    /** Thông điệp mô tả thân thiện - có thể hiển thị trên UI. */
    private final String message;

    /**
     * Constructor đầy đủ - dùng cho event có liên quan đến bid.
     */
    public AuctionEvent(EventType type, String auctionId, BidTransaction transaction, String message) {
        this.type = type;
        this.auctionId = auctionId;
        this.transaction = transaction;
        this.message = message;
    }

    /**
     * Constructor rút gọn - cho event không liên quan đến bid (vd: STARTED, ENDED).
     * Delegate sang constructor đầy đủ với transaction = null.
     */
    public AuctionEvent(EventType type, String auctionId, String message) {
        this(type, auctionId, null, message);
    }

    // ===== GETTERS (không có setter vì class immutable) =====

    public EventType getType() {
        return type;
    }

    public String getAuctionId() {
        return auctionId;
    }

    /** Có thể null nếu event không liên quan đến bid. */
    public BidTransaction getTransaction() {
        return transaction;
    }

    public String getMessage() {
        return message;
    }
}
