package com.auction.pattern.singleton;

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
 * AUCTIONMANAGER - SINGLETON QUẢN LÝ TOÀN BỘ PHIÊN ĐẤU GIÁ
 * ============================================================================
 *
 * <p>Đây là class áp dụng <b>SINGLETON PATTERN</b> - chỉ tồn tại 1 instance
 * duy nhất trong toàn JVM. Vai trò: quản lý "in-memory cache" của các phiên
 * đấu giá đang hoạt động.
 *
 * <p><b>SINGLETON PATTERN LÀ GÌ?</b> Đảm bảo 1 class chỉ có 1 instance, cung
 * cấp 1 điểm truy cập toàn cục. Áp dụng khi:
 * <ul>
 *   <li>Cần quản lý tài nguyên dùng chung (DB connection, cache, scheduler...)</li>
 *   <li>Tránh tạo nhiều instance lãng phí bộ nhớ</li>
 *   <li>Cần một "registry" trung tâm</li>
 * </ul>
 *
 * <p><b>TẠI SAO DÙNG ConcurrentHashMap?</b>
 * Trong môi trường multi-thread (server xử lý nhiều client cùng lúc),
 * HashMap thường sẽ gây ConcurrentModificationException. ConcurrentHashMap
 * an toàn cho việc đọc/ghi song song mà không cần lock toàn bộ.
 *
 */
public class AuctionRegistry {

    /**
     * Instance Singleton.
     * <b>volatile</b> đảm bảo các thread nhìn thấy giá trị mới nhất (memory visibility).
     */
    private static volatile AuctionRegistry instance;

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
    private AuctionRegistry() {
        // Khởi tạo Guava Cache: Tự động đuổi khứ bản ghi ra khỏi RAM nếu sau 10 phút không ai đọc/ghi
        this.auctionCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    /**
     * Lấy instance Singleton - dùng Double-Checked Locking (DCL).
     *
     * <p><b>DCL pattern:</b>
     * <ul>
     *   <li>Lần 1 (ngoài lock): nếu instance đã có → trả về luôn (không tốn lock)</li>
     *   <li>Vào lock</li>
     *   <li>Lần 2 (trong lock): kiểm tra lại để tránh 2 thread cùng tạo instance</li>
     * </ul>
     */
    public static AuctionRegistry getInstance() {
        if (instance == null) {
            synchronized (AuctionRegistry.class) {
                if (instance == null) {
                    instance = new AuctionRegistry();
                }
            }
        }
        return instance;
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

    /**
     * Reset instance về null - CHỈ DÙNG TRONG UNIT TEST.
     *
     * <p>Singleton pattern thông thường KHÔNG có resetInstance() vì phá vỡ
     * tính "duy nhất". Tuy nhiên trong test, mỗi test cần state sạch nên
     * cần reset. Đánh dấu rõ là chỉ dùng trong test để tránh lạm dụng.
     */
    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }
}
