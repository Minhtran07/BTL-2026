package com.auction.network.server;

import com.auction.model.transaction.BidTransaction;
import com.auction.network.message.Message;
import com.auction.network.message.push.AuctionEventPush;
import com.auction.network.message.push.BidUpdatePush;
import com.auction.pattern.observer.AuctionEvent;
import com.auction.pattern.observer.AuctionObserver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ============================================================================
 * PUSHFORWARDER - CẦU NỐI GIỮA OBSERVER PATTERN VÀ NETWORK PUSH
 * ============================================================================
 *
 * <p>Singleton này có vai trò "biến" event từ {@link AuctionEvent} (domain)
 * thành {@link Message} (network protocol) để gửi xuống các client đã subscribe.
 *
 * <p><b>QUY TRÌNH:</b>
 * <ol>
 *   <li>Client gửi SUBSCRIBE_AUCTION → ClientHandler gọi
 *       {@link #subscribe(String, AuctionServer.ClientHandler)}</li>
 *   <li>Khi có bid mới, AuctionService dispatch AuctionEvent qua
 *       AuctionEventDispatcher</li>
 *   <li>PushForwarder đã đăng ký làm observer global → nhận event</li>
 *   <li>{@link #onAuctionEvent(AuctionEvent)} → {@link #dispatch(AuctionEvent)}</li>
 *   <li>dispatch() convert event → Message, gửi cho các ClientHandler subscribe phiên đó</li>
 *   <li>Mỗi ClientHandler.onPush() gọi sendMessage() → socket gửi message về client</li>
 * </ol>
 *
 * <p><b>IMPLEMENTS HAI INTERFACE:</b>
 * <ul>
 *   <li>{@link AuctionObserver} - để đăng ký với AuctionEventDispatcher</li>
 *   <li>(Implicit) Subject của push - quản lý danh sách PushListener</li>
 * </ul>
 *
 * <p><b>THREAD-SAFE:</b>
 * <ul>
 *   <li>ConcurrentHashMap: an toàn cho concurrent put/get</li>
 *   <li>CopyOnWriteArrayList: an toàn cho iteration trong khi có add/remove</li>
 * </ul>
 */
public class PushForwarder implements AuctionObserver {

    private static volatile PushForwarder instance;

    /**
     * Map: auctionId → list các ClientHandler đang subscribe phiên đó.
     *
     * <p>Cấu trúc này cho phép gửi event đến CHỈ những client đang xem
     * phiên cụ thể, thay vì broadcast tất cả (tiết kiệm bandwidth).
     */
    private final Map<String, List<AuctionServer.ClientHandler>> subscriptions;

    private PushForwarder() {
        subscriptions = new ConcurrentHashMap<>();
    }

    /** Lấy instance Singleton (Double-Checked Locking). */
    public static PushForwarder getInstance() {
        if (instance == null) {
            synchronized (PushForwarder.class) {
                if (instance == null) {
                    instance = new PushForwarder();
                }
            }
        }
        return instance;
    }

    /**
     * Đăng ký 1 ClientHandler nhận push event cho 1 auction cụ thể.
     *
     * <p>computeIfAbsent: nếu key chưa có → tạo list mới; nếu có → dùng list cũ.
     * Sau đó add observer vào list.
     *
     * <p>CopyOnWriteArrayList được tạo nếu cần - tối ưu cho việc iteration
     * trong khi có thread khác subscribe/unsubscribe.
     */
    public void subscribe(String auctionId, AuctionServer.ClientHandler observer) {
        subscriptions.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>())
                .add(observer);
    }

    /**
     * Hủy đăng ký observer khỏi 1 auction.
     * Gọi khi client UNSUBSCRIBE hoặc disconnect.
     */
    public void unsubscribe(String auctionId, AuctionServer.ClientHandler observer) {
        List<AuctionServer.ClientHandler> observers = subscriptions.get(auctionId);
        if (observers != null) {
            // Lưu ý: code gốc dùng subscriptions.remove(observer) - có vẻ lỗi
            // (nên là observers.remove(observer))
            subscriptions.remove(observer);
        }
    }

    /**
     * Callback từ AuctionEventDispatcher khi có event.
     * Delegate sang dispatch() để forward cho các client.
     */
    @Override
    public void onAuctionEvent(AuctionEvent event) {
        dispatch(event);
    }

    /**
     * Phát event cho tất cả ClientHandler đang subscribe phiên đó.
     *
     * <p>Try-catch quanh từng callback để 1 client lỗi không làm dừng việc
     * notify các client khác (best-effort delivery).
     */
    private void dispatch(AuctionEvent event) {
        // Lookup các handler đang subscribe phiên này
        List<AuctionServer.ClientHandler> observers = subscriptions.get(event.getAuctionId());
        if (observers != null) {
            for (PushListener observer : observers) {
                try {
                    // Convert event → Message rồi forward
                    observer.onPush(toMessage(event));
                } catch (Exception e) {
                    System.err.println("Error notifying push listener: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Convert {@link AuctionEvent} (domain) → {@link Message} (network protocol).
     *
     * <p>Phân loại:
     * <ul>
     *   <li>NEW_BID / AUTO_BID → BID_UPDATE (client xử lý riêng: cập nhật giá)</li>
     *   <li>Các event khác (STARTED/ENDED/CANCELED/EXTENDED) → AUCTION_EVENT</li>
     * </ul>
     *
     * <p>Đóng gói thông tin từ event vào các key của Message.data:
     * auctionId, eventType, message, bidderId, bidderName, bidAmount, bidTime.
     */
    private static Message toMessage(AuctionEvent event) {
        boolean isBid = event.getType() == AuctionEvent.EventType.NEW_BID
                || event.getType() == AuctionEvent.EventType.AUTO_BID;

        if (isBid) {
            return new BidUpdatePush(
                    event.getAuctionId(),
                    event.getType().name(),
                    event.getTransaction());
        } else {
            return new AuctionEventPush(
                    event.getAuctionId(),
                    event.getType().name(),
                    event.getMessage());
        }
    }
}
