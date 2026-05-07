package com.auction.pattern.singleton;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Singleton Pattern - Quản lý toàn bộ phiên đấu giá.
 * Chỉ có một instance duy nhất trong hệ thống.
 * Tự động kiểm tra và đóng phiên hết thời gian.
 */
public class AuctionManager {

    private static volatile AuctionManager instance;

    private final Map<String, Auction> auctions;
    private final ScheduledExecutorService scheduler;

    private AuctionManager() {
        this.auctions = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        startAuctionMonitor();
    }

    /**
     * Thread-safe Singleton với double-checked locking.
     */
    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    /**
     * Bắt đầu monitor tự động đóng phiên hết hạn.
     */
    private void startAuctionMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                for (Auction auction : auctions.values()) {
                    // Tự động bắt đầu phiên khi đến giờ
                    if (auction.getStatus() == AuctionStatus.OPEN
                        && now.isAfter(auction.getStartTime())) {
                        auction.start();
                    }
                    // Tự động đóng phiên khi hết thời gian
                    if (auction.getStatus() == AuctionStatus.RUNNING
                        && now.isAfter(auction.getEndTime())) {
                        auction.finish();
                    }
                }
            } catch (Exception e) {
                System.err.println("Auction monitor error: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void addAuction(Auction auction) {
        auctions.put(auction.getId(), auction);
    }

    /**
     * Sync với dữ liệu mới từ DAO (dùng cho multi-JVM).
     * Merge sao cho giữ được instance ReentrantLock của auction đang chạy.
     */
    public void syncAuctions(java.util.Collection<Auction> loaded) {
        for (Auction loadedAuction : loaded) {
            Auction existing = auctions.get(loadedAuction.getId());
            if (existing == null) {
                auctions.put(loadedAuction.getId(), loadedAuction);
            } else {
                // Cập nhật các trường có thể thay đổi
                // Không thay đổi ReentrantLock
                existing.setStatus(loadedAuction.getStatus());
                if (loadedAuction.getEndTime() != null) {
                    existing.setEndTime(loadedAuction.getEndTime());
                }
            }
        }
    }

    public Auction getAuction(String auctionId) {
        return auctions.get(auctionId);
    }

    public void removeAuction(String auctionId) {
        auctions.remove(auctionId);
    }

    public List<Auction> getAllAuctions() {
        return List.copyOf(auctions.values());
    }

    public List<Auction> getActiveAuctions() {
        return auctions.values().stream()
            .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
            .collect(Collectors.toList());
    }

    public List<Auction> getAuctionsBySeller(String sellerId) {
        return auctions.values().stream()
            .filter(a -> a.getSellerId().equals(sellerId))
            .collect(Collectors.toList());
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Reset instance cho testing.
     */
    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }
}