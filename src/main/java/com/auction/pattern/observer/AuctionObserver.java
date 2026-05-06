package com.auction.pattern.observer;

/**
 * Observer Pattern - Interface cho các listener.
 * Mỗi client đang xem phiên đấu giá sẽ implement interface này.
 */
public interface AuctionObserver {

    /**
     * Được gọi khi có sự kiện mới trong phiên đấu giá.
     *
     * @param event sự kiện
     */
    void onAuctionEvent(AuctionEvent event);
}