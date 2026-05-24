package com.auction.network.handler;

import com.auction.network.Message;
import com.auction.model.user.User;

/**
 * ============================================================================
 * REQUESTHANDLER - INTERFACE COMMAND PATTERN
 * ============================================================================
 *
 * <p>Đây là interface chung cho TẤT CẢ handler ở server. Mỗi loại request
 * (LOGIN, PLACE_BID, CREATE_ITEM...) có 1 class implement interface này.
 *
 * <p><b>COMMAND PATTERN:</b> Đóng gói 1 "yêu cầu xử lý" thành 1 object. Lợi ích:
 * <ul>
 *   <li>Decoupling: AuctionServer không cần biết logic xử lý cụ thể</li>
 *   <li>Mở rộng dễ: thêm request mới = thêm 1 class implement, không sửa code cũ</li>
 *   <li>Tránh switch-case dài: dùng map {@code Type → Handler}</li>
 * </ul>
 *
 * <p><b>VÍ DỤ:</b>
 * <pre>
 * Map.put(Message.Type.LOGIN, new LoginHandler(userService));
 * Map.put(Message.Type.PLACE_BID, new PlaceBidHandler(auctionService));
 * // Khi có request:
 * Message response = handlerMap.get(request.getType()).handle(request, user);
 * </pre>
 */
public interface RequestHandler {

    /**
     * Xử lý 1 request từ client.
     *
     * @param request           message nhận từ client (chứa type + data + body)
     * @param authenticatedUser user đã đăng nhập trong session (null nếu chưa login)
     * @return message phản hồi gửi về client (SUCCESS hoặc ERROR)
     */
    Message handle(Message request, User authenticatedUser);
}
