package com.auction.pattern.observer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer Pattern - Subject quản lý danh sách observers.
 * Sử dụng CopyOnWriteArrayList để thread-safe notify.
 */
public class AuctionEventDispatcher {

    private static volatile AuctionEventDispatcher instance;

    // Map: auctionId -> list of observers đang theo dõi phiên đó
    private final Map<String, List<AuctionObserver>> auctionObservers;

    // Global observers (theo dõi tất cả phiên)
    private final List<AuctionObserver> globalObservers;

    private AuctionEventDispatcher() {
        this.auctionObservers = new ConcurrentHashMap<>();
        this.globalObservers = new CopyOnWriteArrayList<>();
    }

    public static AuctionEventDispatcher getInstance() {
        if (instance == null) {
            synchronized (AuctionEventDispatcher.class) {
                if (instance == null) {
                    instance = new AuctionEventDispatcher();
                }
            }
        }
        return instance;
    }

    /**
     * Đăng ký observer cho một phiên đấu giá cụ thể.
     */
    public void subscribe(String auctionId, AuctionObserver observer) {
        auctionObservers.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>())
                .add(observer);
    }

    /**
     * Hủy đăng ký observer.
     */
    public void unsubscribe(String auctionId, AuctionObserver observer) {
        List<AuctionObserver> observers = auctionObservers.get(auctionId);
        if (observers != null) {
            observers.remove(observer);
        }
    }

    /**
     * Đăng ký global observer.
     */
    public void subscribeGlobal(AuctionObserver observer) {
        globalObservers.add(observer);
    }

    /**
     * Hủy đăng ký global observer.
     */
    public void unsubscribeGlobal(AuctionObserver observer) {
        globalObservers.remove(observer);
    }

    /**
     * Phát sự kiện - notify tất cả observer đang theo dõi phiên này.
     * Thread-safe nhờ CopyOnWriteArrayList.
     */
    public void dispatch(AuctionEvent event) {
        // Notify observers của phiên cụ thể
        List<AuctionObserver> observers = auctionObservers.get(event.getAuctionId());
        if (observers != null) {
            for (AuctionObserver observer : observers) {
                try {
                    observer.onAuctionEvent(event);
                } catch (Exception e) {
                    System.err.println("Error notifying observer: " + e.getMessage());
                }
            }
        }

        // Notify global observers
        for (AuctionObserver observer : globalObservers) {
            try {
                observer.onAuctionEvent(event);
            } catch (Exception e) {
                System.err.println("Error notifying global observer: " + e.getMessage());
            }
        }
    }

    /**
     * Xóa tất cả observer của một phiên.
     */
    public void clearAuctionObservers(String auctionId) {
        auctionObservers.remove(auctionId);
    }

    public static synchronized void resetInstance() {
        instance = null;
    }
}