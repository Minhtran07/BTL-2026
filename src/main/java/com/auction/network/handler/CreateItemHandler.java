package com.auction.network.handler;

import com.auction.model.item.Item;
import com.auction.model.item.ItemCategory;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.CreateItemRequest;
import com.auction.service.AuctionService;

/**
 * ============================================================================
 * CREATEITEMHANDLER - XỬ LÝ REQUEST TẠO SẢN PHẨM MỚI
 * ============================================================================
 *
 * <p>Quy trình:
 * <ol>
 *   <li>Kiểm tra đã login</li>
 *   <li>Cast request sang {@link CreateItemRequest} (đa hình)</li>
 *   <li>Gọi AuctionService.createItem() - dùng Factory Pattern tạo đúng subclass</li>
 *   <li>Trả về itemId cho client (để dùng tiếp tạo Auction)</li>
 * </ol>
 */
public class CreateItemHandler implements RequestHandler {

    private final AuctionService auctionService;

    public CreateItemHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) return HandlerUtils.error("Chưa đăng nhập");
        if (!(request instanceof CreateItemRequest req)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        ItemCategory category = ItemCategory.valueOf(req.getCategory());

        Item item = auctionService.createItem(
                category,
                req.getName(),
                req.getDescription(),
                req.getPrice(),
                authenticatedUser.getId(),
                req.getExtraFields());

        return Response.success(item.getId());
    }
}
