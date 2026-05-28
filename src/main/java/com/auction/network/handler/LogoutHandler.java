package com.auction.network.handler;

import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;

/**
 * ============================================================================
 * LOGOUTHANDLER - XỬ LÝ REQUEST ĐĂNG XUẤT
 * ============================================================================
 *
 * <p>Handler này chỉ trả SUCCESS đơn giản. Việc xóa {@code authenticatedUser}
 * trong session do ClientHandler tự thực hiện (xem code AuctionServer:
 * sau khi handle() trả về, nếu request là LogoutRequest thì set
 * authenticatedUser = null và unsubscribe tất cả).
 */
public class LogoutHandler implements RequestHandler {

    @Override
    public Message handle(Message request, User authenticatedUser) {
        return Response.success();
    }
}
