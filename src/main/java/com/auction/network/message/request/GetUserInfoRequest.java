package com.auction.network.message.request;

/**
 * Request lấy thông tin user mới nhất từ server (balance, revenue...).
 *
 * <p>Dùng cho màn hình Tài chính — client cần data fresh từ DB thay vì
 * bản cache cũ trong {@code SessionManager}. Nếu {@code userId} là null,
 * server trả về user đang đăng nhập (authenticatedUser).
 */
public class GetUserInfoRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String userId;

    /** Lấy thông tin user đang đăng nhập. */
    public GetUserInfoRequest() {
        this.userId = null;
    }

    /** Lấy thông tin user theo ID (admin dùng). */
    public GetUserInfoRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() { return userId; }
}
