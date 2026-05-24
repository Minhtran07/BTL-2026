package com.auction.pattern.observer;

/**
 * ============================================================================
 * INTERFACE AUCTIONOBSERVER - OBSERVER PATTERN
 * ============================================================================
 *
 * <p>Đây là interface "Observer" trong <b>OBSERVER PATTERN</b> (mẫu Người quan
 * sát). Bất kỳ class nào muốn "nghe ngóng" các sự kiện của phiên đấu giá đều
 * phải implement interface này.
 *
 * <p><b>OBSERVER PATTERN LÀ GÌ?</b>
 * Một design pattern cho phép:
 * <ul>
 *   <li>1 đối tượng (Subject) có nhiều "người theo dõi" (Observer)</li>
 *   <li>Khi Subject có thay đổi → tự động báo cho tất cả Observer</li>
 *   <li>Subject KHÔNG biết Observer là ai cụ thể - chỉ biết interface chung</li>
 * </ul>
 *
 * <p><b>VÍ DỤ TRONG DỰ ÁN NÀY:</b>
 * <ul>
 *   <li>Khi user A đặt bid mới trên phiên X → server muốn báo cho tất cả
 *       client đang xem phiên X biết</li>
 *   <li>{@link AuctionEventDispatcher} (Subject) sẽ gọi onAuctionEvent()
 *       cho từng observer đã subscribe phiên X</li>
 *   <li>Client (Observer) nhận event → cập nhật UI realtime</li>
 * </ul>
 *
 * <p><b>LỢI ÍCH:</b>
 * <ul>
 *   <li><b>Loose coupling:</b> Subject không phụ thuộc vào lớp cụ thể của Observer</li>
 *   <li><b>Dynamic:</b> Có thể thêm/bớt Observer lúc runtime</li>
 *   <li><b>Broadcast:</b> 1 sự kiện → nhiều listener cùng nhận</li>
 * </ul>
 *
 * <p><b>FUNCTIONAL INTERFACE:</b> chỉ có 1 method abstract → có thể dùng
 * lambda expression: {@code dispatcher.subscribe(id, event -> {...});}
 */
public interface AuctionObserver {

    /**
     * Callback được gọi khi có sự kiện mới trong phiên đấu giá.
     *
     * <p>Implementation phải xử lý NHANH - vì nhiều observer cùng listen,
     * 1 observer chậm sẽ làm chậm cả dispatcher (synchronous notify).
     * Nếu cần xử lý nặng → đẩy vào background thread.
     *
     * @param event đối tượng chứa thông tin sự kiện (loại, auctionId, transaction...)
     */
    void onAuctionEvent(AuctionEvent event);
}
