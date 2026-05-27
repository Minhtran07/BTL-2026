package com.auction.network.handler;

import com.auction.exception.AuthenticationException;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.DeactivateUserRequest;
import com.auction.service.UserService;

/**
 * ============================================================================
 * DEACTIVATEUSERHANDLER - KHÓA TÀI KHOẢN USER (CHỈ ADMIN)
 * ============================================================================
 *
 * <p>Khóa user → set active = false → user không login được nữa.
 * KHÔNG xóa khỏi DB → giữ dữ liệu lịch sử.
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
        if (!(request instanceof DeactivateUserRequest req)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        try {
            userService.deactivateUser(req.getUserId());
            return Response.success();
        } catch (AuthenticationException e) {
            return HandlerUtils.error(e.getMessage());
        }
    }
}
