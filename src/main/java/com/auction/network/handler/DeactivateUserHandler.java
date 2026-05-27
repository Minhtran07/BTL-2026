package com.auction.network.handler;

import com.auction.exception.AuthenticationException;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.message.Message;
import com.auction.service.UserService;

/**
 * ============================================================================
 * DEACTIVATEUSERHANDLER - KHÓA TÀI KHOẢN USER (CHỈ ADMIN)
 * ============================================================================
 *
 * <p>Authorization check: chỉ Admin được gọi.
 * Khóa user → set active = false → user không login được nữa.
 * KHÔNG xóa khỏi DB → giữ dữ liệu lịch sử (bid, sale...).
 */
public class DeactivateUserHandler implements RequestHandler {

    private final UserService userService;

    public DeactivateUserHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getRole() != UserRole.ADMIN) {
            return HandlerUtils.error("Không có quyền truy cập");
        }

        try {
            userService.deactivateUser(request.get("userId"));
            Message response = new Message(Message.Type.SUCCESS);
            response.put("message", "Đã khóa tài khoản");
            return response;
        } catch (AuthenticationException e) {
            return HandlerUtils.error(e.getMessage());
        }
    }
}
