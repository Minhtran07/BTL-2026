package com.auction.network.handler;

import com.auction.exception.AuthenticationException;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.service.UserService;

/**
 * ============================================================================
 * REGISTERHANDLER - XỬ LÝ REQUEST ĐĂNG KÝ TÀI KHOẢN
 * ============================================================================
 *
 * <p>Quy trình:
 * <ol>
 *   <li>Parse role từ request</li>
 *   <li>CHẶN đăng ký với role ADMIN (bảo mật)</li>
 *   <li>Gọi UserService.register() - validate + hash + lưu DB</li>
 *   <li>Trả về full User object</li>
 * </ol>
 *
 * <p><b>Tại sao chặn ADMIN?</b> Không cho user tự đăng ký Admin - tránh
 * leo thang quyền. Admin phải được tạo qua DB script hoặc bởi Admin khác.
 */
public class RegisterHandler implements RequestHandler {

    private final UserService userService;

    public RegisterHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        try {
            // Parse role từ String → enum
            UserRole role = UserRole.valueOf(request.get("role"));

            // Chặn đăng ký Admin qua UI
            if (role == UserRole.ADMIN) {
                return HandlerUtils.error("Không được phép đăng ký với vai trò Admin");
            }

            // Gọi service đăng ký (sẽ validate + hash password)
            User user = userService.register(
                    request.get("username"),
                    request.get("password"),
                    request.get("email"),
                    request.get("fullName"),
                    role);

            // Trả về full user qua body
            Response response = Response.success();
            response.setBody(user);
            response.put("userId",  user.getId());
            response.put("message", "Đăng ký thành công");
            return response;
        } catch (AuthenticationException e) {
            // Validate fail (trùng username, password yếu...)
            return HandlerUtils.error(e.getMessage());
        }
    }
}
