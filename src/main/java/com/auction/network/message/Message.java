package com.auction.network.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * MESSAGE - GIAO THỨC TRUYỀN TIN CLIENT-SERVER
 * ============================================================================
 *
 * <p>Đây là "khung tin" (message envelope) dùng để client và server trao đổi
 * thông tin qua TCP socket. Cả 2 đầu đều dùng cùng 1 lớp này (cùng package),
 * gửi bằng Java Serialization (ObjectOutputStream).
 *
 * <p><b>CẤU TRÚC MESSAGE:</b>
 * <ul>
 *   <li>{@code type}: loại request/response (LOGIN, PLACE_BID, ERROR...)</li>
 *   <li>{@code data}: map các tham số đơn giản (String→String)</li>
 *   <li>{@code payload}: chuỗi tự do (legacy)</li>
 *   <li>{@code body}: object phức tạp (Auction, List, User...)</li>
 * </ul>
 *
 * <p><b>VÍ DỤ FLOW:</b>
 * <pre>
 * // CLIENT gửi:
 * Message req = new Message(Type.PLACE_BID);
 * req.put("auctionId", "abc-123");
 * req.put("amount", "150000");
 * out.writeObject(req);
 *
 * // SERVER nhận, xử lý, trả về:
 * Message res = new Message(Type.SUCCESS);
 * res.setBody(transaction);  // object BidTransaction
 * clientOut.writeObject(res);
 * </pre>
 *
 * <p><b>TẠI SAO IMPLEMENTS Serializable?</b>
 * Java Serialization cho phép gửi object qua stream (file, socket). Mọi class
 * cần truyền qua mạng đều phải implement Serializable. Cả các field trong
 * object cũng phải Serializable.
 *
 * <p><b>serialVersionUID = 2L:</b> tăng từ 1L lên 2L khi đổi structure
 * (thêm field body). Nếu client và server build version khác nhau →
 * InvalidClassException nếu UID khác. Đặt cố định để control.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 2L;

    /**
     * Enum liệt kê TẤT CẢ các loại message trong hệ thống.
     *
     * <p>Mỗi type tương ứng với 1 handler ở server.
     * Group theo chức năng:
     */
    public enum Type {
        // ===== AUTHENTICATION =====
        LOGIN, REGISTER, LOGOUT,

        // ===== ITEM CRUD =====
        CREATE_ITEM, GET_ITEM, GET_ALL_ITEMS, UPDATE_ITEM, DELETE_ITEM,

        // ===== AUCTION CRUD =====
        CREATE_AUCTION, GET_AUCTION, GET_ALL_AUCTIONS, GET_ACTIVE_AUCTIONS,
        GET_SELLER_AUCTIONS, END_AUCTION, CANCEL_AUCTION,

        // ===== BIDDING =====
        PLACE_BID, REGISTER_AUTO_BID, GET_BID_HISTORY,

        // ===== SUBSCRIBE PHIÊN (Observer Pattern qua socket) =====
        // Client gửi để báo "tôi đang xem phiên X, hãy push event của X cho tôi"
        SUBSCRIBE_AUCTION, UNSUBSCRIBE_AUCTION,

        // ===== PUSH TỪ SERVER → CLIENT =====
        // Server tự gửi khi có event, không phải response của request
        BID_UPDATE,        // Có bid mới ở phiên X
        AUCTION_EVENT,     // Event tổng quát (started/ended/canceled...)

        // ===== RESPONSE STATUS =====
        SUCCESS,           // Server xử lý OK
        ERROR,             // Có lỗi - chi tiết trong payload

        // ===== ADMIN =====
        GET_ALL_USERS, DEACTIVATE_USER
    }

    /** Loại message. */
    private Type type;

    /**
     * Map các tham số String→String (ví dụ: "auctionId" → "abc-123").
     * Dùng cho data đơn giản, không cần Serializable phức tạp.
     */
    private Map<String, String> data;

    /** Chuỗi tự do (legacy - các phiên bản cũ dùng). */
    private String payload;

    /**
     * Object payload phức tạp - Auction, Item, User, List<Auction>...
     * Vì Message đã đi qua ObjectOutputStream → body cũng được serialize tự động.
     */
    private Serializable body;

    /** Constructor mặc định - cần cho Java Serialization. */
    public Message() {
        this.data = new HashMap<>();
    }

    /** Constructor với type. */
    public Message(Type type) {
        this.type = type;
        this.data = new HashMap<>();
    }

    /** Constructor với type và payload string. */
    public Message(Type type, String payload) {
        this.type = type;
        this.data = new HashMap<>();
        this.payload = payload;
    }

    // ===== GETTERS / SETTERS =====

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public Map<String, String> getData() { return data; }
    public void setData(Map<String, String> data) { this.data = data; }

    /** Helper: đặt 1 cặp key-value vào data map. */
    public void put(String key, String value) {
        data.put(key, value);
    }

    /** Helper: lấy value từ data map (null nếu không có). */
    public String get(String key) {
        return data.get(key);
    }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Serializable getBody() { return body; }
    public void setBody(Serializable body) { this.body = body; }

    /**
     * Helper: lấy body và cast tự động sang kiểu T.
     *
     * <p>Dùng generic + @SuppressWarnings để code gọn:
     * <pre>{@code
     * List<Auction> auctions = response.body();  // không cần (List<Auction>) cast
     * }</pre>
     *
     * <p>Nếu body không khớp kiểu → ClassCastException khi gán.
     */
    @SuppressWarnings("unchecked")
    public <T> T body() {
        return (T) body;
    }

    /**
     * Override toString() để dễ log/debug.
     * Chỉ in tên class của body (không in full content - tránh log dài).
     */

    public String getId();
    @Override
    public String toString() {
        return "Message{type=" + type + ", data=" + data
                + (body != null ? ", body=" + body.getClass().getSimpleName() : "")
                + "}";
    }
}
