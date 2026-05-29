package com.auction.network.handler;

import com.auction.exception.AuthenticationException;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.RegisterRequest;
import com.auction.service.UserService;

/**
 * ============================================================================
 * REGISTERHANDLER - XỬ LÝ REQUEST ĐĂNG KÝ TÀI KHOẢN
 * ============================================================================
 *
 * <p>Quy trình:
 * <ol>
 *   <li>Parse role từ request (BUYER / SELLER)</li>
 *   <li>CHẶN đăng ký với role ADMIN (bảo mật)</li>
 *   <li>Gọi UserService.register() - validate + hash password + lưu DB</li>
 *   <li>Trả về full User object qua {@code Response.success(user)}</li>
 * </ol>
 *
 * <p><b>Tại sao chặn ADMIN?</b> Không cho user tự đăng ký Admin - tránh
 * leo thang quyền. Admin phải được tạo qua DB script hoặc bởi Admin khác.
 *
 * <p><b>Refactoring:</b>
 * <ul>
 *   <li>Trước: {@code request.get("username")}, {@code request.get("password")}...
 *       từ data map, mỗi field phải ép kiểu (String)</li>
 *   <li>Sau: {@code instanceof RegisterRequest req} (pattern matching Java 16+)
 *       → truy cập trực tiếp {@code req.getUsername()}, type-safe</li>
 *   <li>Response: {@code Response.success(user)} thay vì đặt vào data map</li>
 * </ul>
 */
public class RegisterHandler implements RequestHandler {

    private final UserService userService;

    public RegisterHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (!(request instanceof RegisterRequest req)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        try {
            UserRole role = UserRole.valueOf(req.getRole());

            if (role == UserRole.ADMIN) {
                return HandlerUtils.error("Không được phép đăng ký với vai trò Admin");
            }

            User user = userService.register(
                    req.getUsername(),
                    req.getPassword(),
                    req.getEmail(),
                    req.getFullName(),
                    role);

            return Response.success(user);
        } catch (AuthenticationException e) {
            return HandlerUtils.error(e.getMessage());
        }
    }
}
