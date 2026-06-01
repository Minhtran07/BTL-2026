package com.auction.network.handler;

import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.GetUserInfoRequest;
import com.auction.service.UserService;

import java.util.Optional;

/**
 * ============================================================================
 * GETUSERINFOHANDLER - LẤY THÔNG TIN USER MỚI NHẤT TỪ DB
 * ============================================================================
 *
 * <p>Trả về User object fresh từ database — bao gồm balance (Bidder) hoặc
 * totalRevenue (Seller) cập nhật mới nhất. Client dùng cho màn hình Tài chính
 * để kiểm tra số dư/doanh thu sau settlement.
 *
 * <p><b>Logic:</b>
 * <ul>
 *   <li>Nếu request không có userId → trả về user đang đăng nhập (từ session)</li>
 *   <li>Nếu request có userId → lookup theo ID (cho admin xem user khác)</li>
 * </ul>
 */
public class GetUserInfoHandler implements RequestHandler {

    private final UserService userService;

    public GetUserInfoHandler(UserService userService) {
        this.userService = userService;
    }

    /**
     * Trả User object mới nhất từ DB.
     *
     * <p>Nếu userId null → trả authenticatedUser từ DB (fresh, không phải
     * bản cache trong session). Nếu userId có giá trị → lookup theo ID.
     */
    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) {
            return HandlerUtils.error("Chưa đăng nhập");
        }

        if (!(request instanceof GetUserInfoRequest userInfoReq)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        String targetId = userInfoReq.getUserId();
        if (targetId == null) {
            // Lấy chính user đang đăng nhập — dùng ID để query fresh từ DB
            targetId = authenticatedUser.getId();
        }

        Optional<User> userOpt = userService.findById(targetId);
        if (userOpt.isEmpty()) {
            return HandlerUtils.error("Không tìm thấy người dùng");
        }

        return Response.success(userOpt.get());
    }
}
