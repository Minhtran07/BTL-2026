package com.auction.network.handler;

import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.UpdateItemRequest;
import com.auction.service.AuctionService;

/**
 * ============================================================================
 * UPDATEITEMHANDLER - CẬP NHẬT ITEM
 * ============================================================================
 *
 * <p>Client gửi full Item object qua {@link UpdateItemRequest}. Handler:
 * <ol>
 *   <li>Kiểm tra đã login</li>
 *   <li>Kiểm tra quyền: chỉ seller của item hoặc Admin được sửa</li>
 *   <li>Gọi service update</li>
 * </ol>
 */
public class UpdateItemHandler implements RequestHandler {

    private final AuctionService auctionService;

    public UpdateItemHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) return HandlerUtils.error("Chưa đăng nhập");
        if (!(request instanceof UpdateItemRequest req)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        Item item = req.getItem();
        if (item == null) return HandlerUtils.error("Thiếu dữ liệu sản phẩm");

        if (!item.getSellerId().equals(authenticatedUser.getId())
                && authenticatedUser.getRole() != UserRole.ADMIN) {
            return HandlerUtils.error("Bạn không có quyền chỉnh sửa sản phẩm này");
        }

        auctionService.updateItem(item);
        return Response.success();
    }
}
