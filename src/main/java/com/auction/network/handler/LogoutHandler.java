package com.auction.network.handler;

import com.auction.model.user.User;
import com.auction.network.message.Message;

/**
 * ============================================================================
 * LOGOUTHANDLER - XỬ LÝ REQUEST ĐĂNG XUẤT
 * ============================================================================
 *
 * <p>Handler này chỉ trả SUCCESS đơn giản. Việc xóa {@code authenticatedUser}
 * trong session do ClientHandler tự thực hiện (xem code AuctionServer:
 * sau khi handle() trả về, nếu type là LOGOUT thì set authenticatedUser = null
 * và unsubscribe tất cả).
 *
 * <p>Tại sao chia thành 2 chỗ? Vì:
 * <ul>
 *   <li>Handler không có quyền truy cập state của ClientHandler</li>
 *   <li>Cleanup session là trách nhiệm của ClientHandler (đóng gói tốt hơn)</li>
 * </ul>
 */
public class LogoutHandler implements RequestHandler {

    @Override
    public Message handle(Message request, User authenticatedUser) {
        Message response = new Message(Message.Type.SUCCESS);
        response.put("message", "Đã đăng xuất");
        return response;
    }
}
