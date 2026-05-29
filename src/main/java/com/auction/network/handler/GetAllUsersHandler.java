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
 * <p>Authorization check: chỉ Admin được gọi. Trả về danh sách User cho
 * trang quản lý admin.
 *
 * <p><b>Refactoring:</b>
 * <ul>
 *   <li>Trước: đặt {@code List<User>} vào data map với key "users"</li>
 *   <li>Sau: {@code Response.success((Serializable) list)} — body chứa
 *       trực tiếp danh sách, client cast từ {@code response.getBody()}</li>
 * </ul>
 *
 * <p><b>Lưu ý cast:</b> {@code (Serializable)} cần vì Java erasure —
 * {@code List<User>} không implements Serializable trực tiếp, nhưng
 * {@code ArrayList<User>} (từ service) thì có. Cast đảm bảo compile.
 */
public class GetAllUsersHandler implements RequestHandler {

    private final UserService userService;

    public GetAllUsersHandler(UserService userService) {
        this.userService = userService;
    }

    /**
     * Trả danh sách tất cả user. Authorization: chỉ ADMIN.
     * Body: {@code ArrayList<User>} (Serializable).
     */
    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getRole() != UserRole.ADMIN) {
            return HandlerUtils.error("Không có quyền truy cập");
        }

        return Response.success((Serializable) userService.findAll());
    }
}
