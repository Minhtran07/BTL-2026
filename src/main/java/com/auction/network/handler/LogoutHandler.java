package com.auction.network.handler;

import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;

/**
 * ============================================================================
 * LOGOUTHANDLER - XỬ LÝ REQUEST ĐĂNG XUẤT
 * ============================================================================
 *
 * <p>Handler này chỉ trả {@code Response.success()} đơn giản. Việc xóa
 * {@code authenticatedUser} trong session do {@code ClientHandler} tự thực hiện
 * (xem code AuctionServer: sau khi handle() trả về, nếu request là
 * LogoutRequest thì set authenticatedUser = null và unsubscribe tất cả).
 *
 * <p><b>Refactoring:</b> Trước đây trả {@code new Message(Type.LOGOUT_RESPONSE)}.
 * Giờ trả {@code Response.success()} — không body, chỉ báo thành công.
 * Đây là handler đơn giản nhất — không cần validate, không cần quyền.
 */
public class LogoutHandler implements RequestHandler {

    @Override
    public Message handle(Message request, User authenticatedUser) {
        return Response.success();
    }
}
