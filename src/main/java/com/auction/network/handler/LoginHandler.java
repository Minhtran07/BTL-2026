package com.auction.network.handler;

import com.auction.exception.AuthenticationException;
import com.auction.model.user.User;
import com.auction.network.Message;
import com.auction.service.UserService;

/**
 * ============================================================================
 * LOGINHANDLER - XỬ LÝ REQUEST LOGIN
 * ============================================================================
 *
 * <p>Khi client gửi LOGIN:
 * <ol>
 *   <li>Đọc username, password từ request</li>
 *   <li>Gọi UserService.login() - xác thực + check active</li>
 *   <li>Nếu OK: trả full User object qua body → client có sẵn data dùng</li>
 *   <li>Nếu fail: trả ERROR với message lỗi</li>
 * </ol>
 *
 * <p><b>LẠI LƯU lastAuthenticatedUser?</b>
 * Vì ClientHandler bên ngoài cần biết user vừa login để cập nhật session
 * ({@code authenticatedUser}). Sau LOGIN thành công, ClientHandler gọi
 * {@link #getLastAuthenticatedUser()} để lấy User.
 *
 * <p><b>volatile:</b> đảm bảo thread khác (ClientHandler) thấy được giá trị
 * mới nhất sau khi handle() set vào.
 */
public class LoginHandler implements RequestHandler {

    private final UserService userService;
    /** Lưu tạm user vừa login để ClientHandler lấy ra cập nhật session. */
    private volatile User lastAuthenticatedUser;

    /** Inject UserService qua constructor (Dependency Injection). */
    public LoginHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        try {
            // Gọi service xác thực (throw AuthenticationException nếu fail)
            User user = userService.login(request.get("username"), request.get("password"));
            this.lastAuthenticatedUser = user; // lưu để ClientHandler lấy

            // Build response SUCCESS với full user object
            Message response = new Message(Message.Type.SUCCESS);
            response.setBody(user);  // toàn bộ object qua body
            response.put("userId", user.getId());
            response.put("role", user.getRole().name());
            return response;
        } catch (AuthenticationException e) {
            // Sai username/password / tài khoản bị khóa
            return HandlerUtils.error(e.getMessage());
        }
    }

    /** Trả về user vừa login - ClientHandler dùng để cập nhật session. */
    public User getLastAuthenticatedUser() {
        return lastAuthenticatedUser;
    }
}
