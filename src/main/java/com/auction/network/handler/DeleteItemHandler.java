package com.auction.network.handler;

import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.DeleteItemRequest;
import com.auction.service.AuctionService;

import java.util.Optional;

/**
 * ============================================================================
 * DELETEITEMHANDLER - XÓA ITEM
 * ============================================================================
 *
 * <p>Kiểm tra quyền trước khi xóa. Chỉ seller hoặc Admin được phép xóa.
 */
public class DeleteItemHandler implements RequestHandler {

    private final AuctionService auctionService;

    public DeleteItemHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) return HandlerUtils.error("Chưa đăng nhập");
        if (!(request instanceof DeleteItemRequest req)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        Optional<Item> opt = auctionService.getItem(req.getItemId());
        if (opt.isEmpty()) return HandlerUtils.error("Không tìm thấy sản phẩm");

        Item item = opt.get();
        if (!item.getSellerId().equals(authenticatedUser.getId())
                && authenticatedUser.getRole() != UserRole.ADMIN) {
            return HandlerUtils.error("Bạn không có quyền xóa sản phẩm này");
        }

        auctionService.deleteItem(req.getItemId());
        return Response.success();
    }
}
