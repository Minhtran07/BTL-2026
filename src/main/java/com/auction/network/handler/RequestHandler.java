package com.auction.network.handler;

import com.auction.network.message.Message;
import com.auction.model.user.User;

/**
 * ============================================================================
 * REQUESTHANDLER - INTERFACE COMMAND PATTERN
 * ============================================================================
 *
 * <p>Đây là interface chung cho TẤT CẢ handler ở server. Mỗi loại request
 * (LoginRequest, PlaceBidRequest, CreateItemRequest...) có 1 class implement.
 *
 * <p><b>COMMAND PATTERN:</b> Đóng gói 1 "yêu cầu xử lý" thành 1 object. Lợi ích:
 * <ul>
 *   <li>Decoupling: AuctionServer không cần biết logic xử lý cụ thể</li>
 *   <li>Mở rộng dễ: thêm request mới = thêm 1 class implement, không sửa code cũ</li>
 *   <li>Dispatch đa hình: dùng {@code Map<Class<? extends Message>, Handler>}</li>
 * </ul>
 *
 * <p><b>VÍ DỤ:</b>
 * <pre>
 * map.put(LoginRequest.class, new LoginHandler(userService));
 * map.put(PlaceBidRequest.class, new PlaceBidHandler(auctionService));
 * // Khi có request:
 * Message response = handlerMap.get(request.getClass()).handle(request, user);
 * </pre>
 */
public interface RequestHandler {

    /**
     * Xử lý 1 request từ client.
     *
     * @param request           message nhận từ client (subclass cụ thể của Request)
     * @param authenticatedUser user đã đăng nhập trong session (null nếu chưa login)
     * @return message phản hồi gửi về client (Response thành công hoặc lỗi)
     */
    Message handle(Message request, User authenticatedUser);
}
