package com.auction.pattern.singleton.observer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer Pattern - <b>Subject</b> (concrete) quản lý và phát sự kiện đến
 * các {@link AuctionObserver} đăng ký.
 *
 * <h3>Vai trò trong pattern</h3>
 * <ul>
 *   <li><b>Subject</b>: lớp này — giữ danh sách observer, cung cấp API
 *       {@link #subscribe}, {@link #unsubscribe}, {@link #dispatch}.</li>
 *   <li><b>Observer</b>: interface {@link AuctionObserver}.</li>
 *   <li><b>Event</b>: {@link AuctionEvent} (data class chứa loại sự kiện,
 *       auctionId, transaction, message).</li>
 * </ul>
 *
 * <h3>Thiết kế 2 kênh đăng ký</h3>
 * <ol>
 *   <li><b>Per-auction observers</b> — qua {@link #subscribe}. Mỗi observer chỉ
 *       nhận event của một phiên đấu giá cụ thể. Dùng cho client mở màn hình
 *       chi tiết phiên: chỉ cần realtime cập nhật phiên đó.</li>
 *   <li><b>Global observers</b> — qua {@link #subscribeGlobal}. Observer nhận
 *       mọi event của mọi phiên. Dùng cho admin dashboard, audit log, statistics.</li>
 * </ol>
 *
 * <h3>Thread-safety</h3>
 * <ul>
 *   <li>{@link ConcurrentHashMap} cho map per-auction để 2 thread {@link #subscribe}
 *       và {@link #dispatch} không lock nhau.</li>
 *   <li>{@link CopyOnWriteArrayList} cho từng list observer: iteration trong
 *       {@link #dispatch} không bị {@code ConcurrentModificationException} dù có
 *       observer mới {@code subscribe()}/{@code unsubscribe()} đồng thời. Trade-off:
 *       mỗi lần subscribe/unsubscribe copy lại array — phù hợp khi reads (dispatch)
 *       nhiều hơn writes (subscribe).</li>
 *   <li>Lỗi của 1 observer (throw exception) <em>không</em> chặn việc notify các
 *       observer khác — try/catch bao quanh từng callback.</li>
 * </ul>
 *
 * <h3>Singleton</h3>
 * Lớp này áp dụng <b>double-checked locking singleton</b> để cả app share cùng
 * 1 bus event. {@link #resetInstance()} dùng cho unit test (tránh state lẫn
 * lộn giữa các test).
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

    /**
     * Trả về singleton instance, lazily-initialized với double-checked locking.
     *
     * <p>Lý do dùng double-checked locking thay vì static initializer:
     * static initializer sẽ tạo dispatcher ngay khi class được load — gây side
     * effect cho test (instance còn observers cũ giữa các test). Lazy init
     * kết hợp với {@link #resetInstance()} cho phép control rõ ràng vòng đời.
     *
     * @return instance dùng chung cho toàn app
     */
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
     * Đăng ký observer nhận event của một phiên đấu giá cụ thể.
     *
     * <p>Một observer có thể subscribe nhiều phiên (gọi {@code subscribe()}
     * nhiều lần với {@code auctionId} khác nhau). Nếu cùng observer subscribe
     * cùng phiên 2 lần, observer sẽ nhận event 2 lần (deduplication là trách
     * nhiệm của caller).
     *
     * @param auctionId id phiên đấu giá cần theo dõi
     * @param observer  observer nhận callback {@link AuctionObserver#onAuctionEvent}
     */
    public void subscribe(String auctionId, AuctionObserver observer) {
        auctionObservers.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>())
                .add(observer);
    }

    /**
     * Huỷ đăng ký observer của một phiên cụ thể.
     *
     * <p>No-op nếu phiên chưa có observer nào hoặc observer chưa subscribe phiên này.
     * Caller phải gọi method này khi client rời màn hình chi tiết — nếu không
     * sẽ leak observer (đặc biệt nguy hiểm với UI listener giữ scene/window
     * reference).
     *
     * @param auctionId id phiên đã subscribe trước đó
     * @param observer  observer cần huỷ
     */
    public void unsubscribe(String auctionId, AuctionObserver observer) {
        List<AuctionObserver> observers = auctionObservers.get(auctionId);
        if (observers != null) {
            observers.remove(observer);
        }
    }

    /**
     * Đăng ký observer nhận <em>mọi</em> event của <em>mọi</em> phiên.
     *
     * <p>Dùng cho dashboard admin, statistics, audit log. Cẩn thận: observer
     * dạng này sẽ nhận lượng event lớn — không nên làm I/O nặng bên trong
     * {@link AuctionObserver#onAuctionEvent}.
     */
    public void subscribeGlobal(AuctionObserver observer) {
        globalObservers.add(observer);
    }

    /**
     * Huỷ đăng ký global observer. No-op nếu chưa subscribe.
     */
    public void unsubscribeGlobal(AuctionObserver observer) {
        globalObservers.remove(observer);
    }

    /**
     * Phát một event tới tất cả observer liên quan.
     *
     * <p>Thứ tự notify:
     * <ol>
     *   <li>Per-auction observers (theo {@link AuctionEvent#getAuctionId()}).</li>
     *   <li>Global observers.</li>
     * </ol>
     *
     * <p>Đặc tính:
     * <ul>
     *   <li><b>Best-effort</b>: nếu 1 observer throw exception, log ra
     *       {@code System.err} và tiếp tục notify các observer còn lại.</li>
     *   <li><b>Synchronous</b>: caller thread thực hiện toàn bộ callback.
     *       Nếu observer chậm, caller sẽ chậm theo. (Có thể nâng cấp sang
     *       async với {@code ExecutorService} nếu thấy hiệu năng có vấn đề.)</li>
     *   <li><b>Thread-safe</b>: nhờ {@link CopyOnWriteArrayList} và
     *       {@link ConcurrentHashMap}.</li>
     * </ul>
     *
     * @param event event cần phát (không được null)
     */
    public void dispatch(AuctionEvent event) {
        // Notify per-auction observers — dùng map lookup O(1)
        List<AuctionObserver> observers = auctionObservers.get(event.getAuctionId());
        if (observers != null) {
            for (AuctionObserver observer : observers) {
                try {
                    observer.onAuctionEvent(event);
                } catch (Exception e) {
                    // Best-effort: lỗi 1 observer không được làm hỏng việc
                    // notify các observer khác (giữ đặc tính fairness)
                    System.err.println("Error notifying observer: " + e.getMessage());
                }
            }
        }

        // Notify global observers (mọi phiên đều được nhận)
        for (AuctionObserver observer : globalObservers) {
            try {
                observer.onAuctionEvent(event);
            } catch (Exception e) {
                System.err.println("Error notifying global observer: " + e.getMessage());
            }
        }
    }

    /**
     * Xoá toàn bộ observer của một phiên (vd: khi phiên đã FINISHED và
     * không cần theo dõi nữa).
     */
    public void clearAuctionObservers(String auctionId) {
        auctionObservers.remove(auctionId);
    }

    /**
     * Đặt lại instance về null — chỉ dùng cho unit test.
     *
     * <p>Test dùng singleton dễ bị "test pollution": state của test A leak
     * sang test B. {@code @BeforeEach resetInstance()} đảm bảo mỗi test bắt
     * đầu với dispatcher sạch.
     */
    public static synchronized void resetInstance() {
        instance = null;
    }
}