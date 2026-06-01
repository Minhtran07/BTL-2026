package com.auction.service;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * AUCTIONREGISTRY - QUẢN LÝ TOÀN BỘ PHIÊN ĐẤU GIÁ
 * ============================================================================
 *
 * <p><b>TẠI SAO DÙNG ConcurrentHashMap?</b>
 * Trong môi trường multi-thread (server xử lý nhiều client cùng lúc),
 * HashMap thường sẽ gây ConcurrentModificationException. ConcurrentHashMap
 * an toàn cho việc đọc/ghi song song mà không cần lock toàn bộ.
 *
 */
public class AuctionRegistry {
    /**
     * Cache các phiên đấu giá trong RAM thay cho ConcurrentHashMap.
     * Key = auctionId, Value = Auction object.
     */
    private final Cache<String, Auction> auctionCache;

    /**
     * Scheduler chạy task định kỳ (kiểm tra phiên hết giờ).
     * Pool 2 thread cho tasks ngắn.
     */
    private final ScheduledExecutorService scheduler;

    /**
     * Constructor PRIVATE - đặc trưng Singleton.
     * Khởi tạo map rỗng + scheduler, sau đó bắt đầu task monitor.
     */
    public AuctionRegistry() {
        // Khởi tạo Guava Cache: Tự động đuổi khứ bản ghi ra khỏi RAM nếu sau 10 phút không ai đọc/ghi
        this.auctionCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    /**
     * BÁO THỨC MỞ PHÒNG
     */
    public void scheduleAuctionStart(long delayInSeconds, Runnable targetTask) {
        scheduler.schedule(() -> {
            try {
                targetTask.run(); // Đến giờ thì kích hoạt hành động được giao
            } catch (Exception e) {
                System.err.println("Lỗi khi chạy task mở phòng: " + e.getMessage());
            }
        }, delayInSeconds, TimeUnit.SECONDS);
    }

    /**
     * BÁO THỨC ĐÓNG PHÒNG
     */
    public void scheduleAuctionEnd(long delayInSeconds, Runnable targetTask) {
        scheduler.schedule(() -> {
            try {
                targetTask.run(); // Đến giờ thì kích hoạt hành động được giao
            } catch (Exception e) {
                System.err.println("Lỗi khi chạy task đóng phòng: " + e.getMessage());
            }
        }, delayInSeconds, TimeUnit.SECONDS);
    }

    /** Thêm 1 phiên đấu giá vào cache. */
    public void addAuction(Auction auction) {
        auctionCache.put(auction.getId(), auction);
    }

    /** Lấy 1 phiên theo id (null nếu không có). */
    public Auction getAuction(String auctionId) {
        return auctionCache.getIfPresent(auctionId);
    }

    /** Xóa phiên khỏi cache (vd khi seller hủy). */
    public void removeAuction(String auctionId) {
        auctionCache.invalidate(auctionId);
    }

    /** Lấy tất cả phiên - trả về list immutable (an toàn). */
    public List<Auction> getAllAuctions() {
        return List.copyOf(auctionCache.asMap().values());
    }

    /** Lấy chỉ các phiên đang RUNNING (dùng cho list màn hình chính). */
    public List<Auction> getActiveAuctions() {
        return auctionCache.asMap().values().stream()
            .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
            .collect(Collectors.toList());
    }

    /** Lấy các phiên của 1 seller cụ thể trên RAM. */
    public List<Auction> getAuctionsBySeller(String sellerId) {
        return auctionCache.asMap().values().stream()
            .filter(a -> a.getSellerId().equals(sellerId))
            .collect(Collectors.toList());
    }

    /**
     * Dừng scheduler khi server tắt.
     * <p><b>Pattern shutdown chuẩn:</b>
     * <ol>
     *   <li>Gọi shutdown() - không nhận task mới, hoàn thành task đang chạy</li>
     *   <li>Đợi 5 giây cho tasks hoàn thành</li>
     *   <li>Nếu chưa xong → shutdownNow() để force kill</li>
     * </ol>
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow(); // force kill
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            // Preserve interrupt flag - quan trọng để thread cha biết bị interrupt
            Thread.currentThread().interrupt();
        }
    }
}
