package com.auction.network.message.push;

import com.auction.model.transaction.BidTransaction;

/**
 * ============================================================================
 * BIDUPDATEPUSH - PUSH EVENT KHI CÓ BID MỚI
 * ============================================================================
 *
 * <p>Push message gửi từ server xuống client khi có bid mới. Server broadcast
 * tới tất cả client đang subscribe phiên này (Observer Pattern).
 *
 * <p><b>Event types:</b>
 * <ul>
 *   <li>{@code "NEW_BID"} — bid thủ công từ user</li>
 *   <li>{@code "AUTO_BID"} — bid tự động từ auto-bid engine</li>
 * </ul>
 *
 * <p><b>Refactoring (từ Message hierarchy mới):</b>
 * <ul>
 *   <li>Trước: dùng {@code new Message(Type.BID_UPDATE)} + data map chứa
 *       từng field ({@code put("eventType", ...), put("bidAmount", ...)})</li>
 *   <li>Sau: extend {@link PushMessage} (abstract, kế thừa {@link com.auction.network.message.Message}),
 *       có typed fields riêng → client dùng {@code instanceof BidUpdatePush}
 *       để phân loại, truy cập getter trực tiếp, type-safe</li>
 * </ul>
 *
 * <p><b>Null-safe constructor:</b> Phòng trường hợp tx = null (lý thuyết
 * không xảy ra, nhưng defensive programming).
 *
 * @see PushMessage
 * @see AuctionEventPush
 */
public class BidUpdatePush extends PushMessage {
    private static final long serialVersionUID = 1L;

    private final String eventType;    // "NEW_BID" hoặc "AUTO_BID"
    private final String bidderId;     // ID người đặt giá
    private final String bidderName;   // Tên hiển thị người đặt giá
    private final double bidAmount;    // Số tiền đã đặt
    private final String bidTime;      // Thời điểm đặt giá (ISO string)

    /**
     * Tạo push event từ BidTransaction.
     * @param auctionId ID phiên đấu giá (kế thừa từ PushMessage)
     * @param eventType "NEW_BID" hoặc "AUTO_BID"
     * @param tx        transaction chứa thông tin bid (null-safe)
     */
    public BidUpdatePush(String auctionId, String eventType, BidTransaction tx) {
        super(auctionId);
        this.eventType = eventType;
        this.bidderId = tx != null ? tx.getBidderId() : null;
        this.bidderName = tx != null ? tx.getBidderName() : null;
        this.bidAmount = tx != null ? tx.getBidAmount() : 0;
        this.bidTime = tx != null ? tx.getBidTime().toString() : null;
    }

    public String getEventType() { return eventType; }
    public String getBidderId() { return bidderId; }
    public String getBidderName() { return bidderName; }
    public double getBidAmount() { return bidAmount; }
    public String getBidTime() { return bidTime; }
}
