package com.auction.network.handler;

import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.service.UserService;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * ============================================================================
 * GETALLUSERSHANDLER - LẤY TẤT CẢ USER (CHỈ ADMIN)
 * ============================================================================
 *
 * <p>Authorization check: chỉ Admin được gọi.
 */
public class GetAllUsersHandler implements RequestHandler {

    private final UserService userService;

    public GetAllUsersHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getRole() != UserRole.ADMIN) {
            return HandlerUtils.error("Không có quyền truy cập");
        }

        return Response.success((Serializable) userService.findAll());
    }
}
