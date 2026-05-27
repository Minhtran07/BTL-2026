package com.auction.network.handler;

import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.service.UserService;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * GETALLUSERSHANDLER - LẤY TẤT CẢ USER (CHỈ ADMIN)
 * ============================================================================
 *
 * <p>Authorization check: chỉ Admin được gọi. Bidder/Seller thông thường
 * không được xem danh sách user khác.
 *
 * <p>Dùng cho Admin panel - hiển thị bảng quản lý user.
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

        List<User> users = new ArrayList<>(userService.findAll());
        Response response = Response.success();
        response.put("count", String.valueOf(users.size()));
        response.setBody((java.io.Serializable) users);
        return response;
    }
}
