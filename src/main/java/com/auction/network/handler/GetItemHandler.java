package com.auction.network.handler;

import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.service.AuctionService;

import java.util.Optional;

/**
 * ============================================================================
 * GETITEMHANDLER - LẤY 1 ITEM THEO ID
 * ============================================================================
 *
 * <p>Trả về full Item object qua body (Electronics/Art/Vehicle).
 * Client cast bằng instanceof để lấy field riêng của từng subtype.
 *
 * <p>Trả ERROR nếu không tìm thấy itemId.
 */
public class GetItemHandler implements RequestHandler {

    private final AuctionService auctionService;

    public GetItemHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        Optional<Item> opt = auctionService.getItem(request.get("itemId"));
        if (opt.isEmpty()) return HandlerUtils.error("Không tìm thấy sản phẩm");

        Message response = new Message(Message.Type.SUCCESS);
        response.setBody(opt.get()); // gửi nguyên object qua body
        return response;
    }
}
