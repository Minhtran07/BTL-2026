package com.auction.network.handler;

import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.message.Message;
import com.auction.service.AuctionService;

/**
 * ============================================================================
 * UPDATEITEMHANDLER - CẬP NHẬT ITEM
 * ============================================================================
 *
 * <p>Client gửi full Item object qua body (đã sửa). Handler:
 * <ol>
 *   <li>Kiểm tra đã login</li>
 *   <li>Kiểm tra quyền: chỉ seller của item hoặc Admin được sửa</li>
 *   <li>Gọi service update</li>
 * </ol>
 *
 * <p><b>AUTHORIZATION CHECK:</b> Quan trọng - không cho user A sửa item của
 * user B. Trừ trường hợp user A là Admin.
 */
public class UpdateItemHandler implements RequestHandler {

    private final AuctionService auctionService;

    public UpdateItemHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) return HandlerUtils.error("Chưa đăng nhập");

        // Lấy Item object từ body
        Item item = request.body();
        if (item == null) return HandlerUtils.error("Thiếu dữ liệu sản phẩm");

        // ===== KIỂM TRA QUYỀN =====
        // User phải là seller của item HOẶC là Admin
        if (!item.getSellerId().equals(authenticatedUser.getId())
                && authenticatedUser.getRole() != UserRole.ADMIN) {
            return HandlerUtils.error("Bạn không có quyền chỉnh sửa sản phẩm này");
        }

        auctionService.updateItem(item);
        Message response = new Message(Message.Type.SUCCESS);
        response.put("message", "Đã lưu thay đổi");
        return response;
    }
}
