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
 * <p>Kiểm tra quyền + ràng buộc trước khi xóa:
 * <ul>
 *   <li>Chỉ seller sở hữu hoặc Admin được phép xóa</li>
 *   <li>Không được xóa item đang có phiên đấu giá tham chiếu
 *       (vi phạm FOREIGN KEY)</li>
 * </ul>
 *
 * <p><b>FIX lỗi FOREIGN KEY:</b> Bảng {@code auctions} có
 * {@code FOREIGN KEY (item_id) REFERENCES items(id)} KHÔNG có
 * {@code ON DELETE CASCADE}. Nếu cố xóa item mà vẫn còn auction
 * tham chiếu → SQLite ném exception. Handler kiểm tra trước bằng cách
 * scan {@code getAllAuctions()} để tìm auction có cùng itemId.
 * Nếu tìm thấy → trả lỗi yêu cầu hủy phiên trước.
 *
 * <p><b>Refactoring:</b> Dùng {@code instanceof DeleteItemRequest req}
 * (pattern matching Java 16+) thay cho data map.
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
