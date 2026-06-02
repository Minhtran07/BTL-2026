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
 * AUCTIONREGISTRY - QUẢN LÝ TOÀN BỘ PHIÊN ĐẤU GIÁ TRÊN RAM
 * ============================================================================
 *
 * <p>Cache in-memory các phiên đấu giá đang hoạt động bằng {@link Cache}
 * (Guava). Tự động evict phiên không được truy cập sau 10 phút, giảm
 * áp lực bộ nhớ mà không cần quản lý vòng đời thủ công.
 *
 * <p><b>TẠI SAO DÙNG GUAVA CACHE?</b>
 * Guava Cache nội bộ dùng ConcurrentHashMap - an toàn cho việc đọc/ghi
 * song song trong môi trường multi-thread (server xử lý nhiều client
 * cùng lúc) mà không cần lock toàn bộ. Ngoài ra còn hỗ trợ tự động
 * eviction theo thời gian truy cập, giúp giải phóng RAM cho các phiên
 * đã kết thúc.
 *
 * <p><b>Lưu ý:</b> Class này KHÔNG phải Singleton. Mỗi {@code AuctionService}
 * sở hữu một instance riêng, cho phép test dễ dàng bằng cách inject
 * instance mới cho mỗi test case.
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
     * Khởi tạo cache rỗng + scheduler cho các task định thời (mở/đóng phòng).
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
