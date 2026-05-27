package com.auction.network.handler;

import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.service.AuctionService;

import java.util.Optional;

/**
 * ============================================================================
 * DELETEITEMHANDLER - XÓA ITEM
 * ============================================================================
 *
 * <p>Tương tự UPDATE - kiểm tra quyền trước khi xóa.
 * Chỉ seller hoặc Admin được phép xóa.
 *
 * <p><b>Lưu ý:</b> Việc kiểm tra "không xóa item có auction đang chạy" đã
 * được kiểm ở phía client (EditItemController) trước khi gửi request.
 * Server tin tưởng nhưng vẫn nên kiểm tra ở đây (defense in depth).
 */
public class DeleteItemHandler implements RequestHandler {

    private final AuctionService auctionService;

    public DeleteItemHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) return HandlerUtils.error("Chưa đăng nhập");

        String itemId = request.get("itemId");
        Optional<Item> opt = auctionService.getItem(itemId);
        if (opt.isEmpty()) return HandlerUtils.error("Không tìm thấy sản phẩm");

        Item item = opt.get();
        if (!item.getSellerId().equals(authenticatedUser.getId())
                && authenticatedUser.getRole() != UserRole.ADMIN) {
            return HandlerUtils.error("Bạn không có quyền xóa sản phẩm này");
        }

        auctionService.deleteItem(itemId);
        Response response = Response.success();
        response.put("message", "Đã xóa sản phẩm");
        return response;
    }
}
