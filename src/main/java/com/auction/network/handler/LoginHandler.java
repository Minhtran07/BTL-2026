package com.auction.network.handler;

import com.auction.exception.AuthenticationException;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.LoginRequest;
import com.auction.service.UserService;

/**
 * ============================================================================
 * LOGINHANDLER - XỬ LÝ ĐĂNG NHẬP
 * ============================================================================
 *
 * <p>Nhận {@link LoginRequest} chứa username/password, xác thực qua
 * {@link UserService} và trả về {@link User} object trong body response.
 *
 * <p><b>Lưu ý:</b> {@link #lastAuthenticatedUser} được lưu tạm để
 * {@code ClientHandler} trong AuctionServer cập nhật session state
 * sau khi login thành công. Dùng volatile vì handler có thể được gọi
 * từ nhiều thread (thread pool xử lý client).
 *
 * <p><b>Refactoring:</b> Trước đây dùng {@code request.get("username")}
 * từ data map chung. Giờ dùng {@code instanceof LoginRequest loginReq}
 * (pattern matching Java 16+) → type-safe tại compile-time, không cần cast.
 */
public class LoginHandler implements RequestHandler {

    private final UserService userService;
    /** User vừa đăng nhập — AuctionServer.ClientHandler đọc để set session. */
    private volatile User lastAuthenticatedUser;

    public LoginHandler(UserService userService) {
        this.userService = userService;
    }

    /**
     * Xác thực user và trả User object nếu thành công.
     *
     * <p>Pattern: {@code instanceof LoginRequest loginReq} — ép kiểu + kiểm tra
     * trong 1 bước (Java 16+ pattern matching for instanceof).
     */
    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (!(request instanceof LoginRequest loginReq)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        try {
            User user = userService.login(loginReq.getUsername(), loginReq.getPassword());
            this.lastAuthenticatedUser = user;
            return Response.success(user); // Body = User object (Serializable)
        } catch (AuthenticationException e) {
            return Response.error(e.getMessage());
        }
    }

    /** Lấy user vừa đăng nhập — chỉ dùng bởi AuctionServer.ClientHandler. */
    public User getLastAuthenticatedUser() {
        return lastAuthenticatedUser;
    }
}
