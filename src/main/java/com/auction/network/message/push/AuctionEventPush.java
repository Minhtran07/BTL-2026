package com.auction.network.message.push;

import com.auction.pattern.observer.AuctionEvent;

/**
 * ============================================================================
 * AUCTIONEVENTPUSH - PUSH EVENT TRẠNG THÁI PHIÊN ĐẤU GIÁ
 * ============================================================================
 *
 * <p>Push message gửi từ server xuống client cho sự kiện thay đổi trạng thái
 * phiên đấu giá. Server broadcast tới tất cả subscriber (Observer Pattern).
 *
 * <p><b>Event types:</b>
 * <ul>
 *   <li>{@code "STARTED"} — phiên bắt đầu (OPEN → RUNNING)</li>
 *   <li>{@code "ENDED"} — phiên kết thúc (RUNNING → FINISHED)</li>
 *   <li>{@code "CANCELED"} — phiên bị hủy (→ CANCELED)</li>
 *   <li>{@code "EXTENDED"} — phiên được gia hạn (anti-sniping)</li>
 * </ul>
 *
 * <p><b>Refactoring:</b>
 * <ul>
 *   <li>Trước: dùng {@code new Message(Type.AUCTION_EVENT)} + data map</li>
 *   <li>Sau: extend {@link PushMessage}, typed fields, client lọc bằng
 *       {@code instanceof AuctionEventPush}</li>
 * </ul>
 *
 * @see PushMessage
 * @see BidUpdatePush
 */
public class AuctionEventPush extends PushMessage {
    private static final long serialVersionUID = 1L;

    private final AuctionEvent.EventType eventType;     // "STARTED", "ENDED", "CANCELED", "EXTENDED"
    private final String eventMessage;  // Mô tả chi tiết sự kiện (hiển thị cho user)

    /**
     * @param auctionId    ID phiên đấu giá
     * @param eventType    loại sự kiện ("STARTED", "ENDED", "CANCELED", "EXTENDED")
     * @param eventMessage mô tả sự kiện (ví dụ: "Phiên đã kết thúc. Người thắng: ...")
     */
    public AuctionEventPush(String auctionId, AuctionEvent.EventType eventType, String eventMessage) {
        super(auctionId);
        this.eventType = eventType;
        this.eventMessage = eventMessage;
    }

    public AuctionEvent.EventType getEventType() { return eventType; }
    public String getEventMessage() { return eventMessage; }
}
