package com.auction.network.handler;

import com.auction.model.transaction.BidTransaction;

/**
 * ============================================================================
 * BIDBROADCASTER - FUNCTIONAL INTERFACE CHO BROADCAST BID
 * ============================================================================
 *
 * <p><b>FUNCTIONAL INTERFACE</b> - chỉ có 1 method abstract → có thể dùng
 * lambda expression thay vì viết anonymous class.
 *
 * <p>Annotation {@code @FunctionalInterface} báo cho compiler kiểm tra và
 * báo lỗi nếu vô tình thêm method abstract thứ 2.
 *
 * <p><b>Mục đích:</b> Cho phép {@link PlaceBidHandler} broadcast bid update
 * mà không cần tham chiếu trực tiếp đến {@code AuctionServer} - giảm coupling.
 *
 * <p><b>Sử dụng:</b>
 * <pre>
 * BidBroadcaster broadcaster = (id, tx) -> server.broadcastBidUpdate(id, tx);
 * </pre>
 *
 * <p><b>Lưu ý:</b> Interface này hiện ít được dùng vì broadcast đã chuyển sang
 * Observer Pattern qua AuctionEventDispatcher → đẩy event ở service layer.
 */
@FunctionalInterface
public interface BidBroadcaster {

    /**
     * Phát BID_UPDATE event cho các client.
     *
     * @param auctionId   ID phiên đấu giá có bid mới
     * @param transaction BidTransaction vừa được tạo
     */
    void broadcast(String auctionId, BidTransaction transaction);
}
