package com.auction.network.handler;

import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.GetItemRequest;
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
 * <p><b>Refactoring:</b>
 * <ul>
 *   <li>Trước: {@code request.get("itemId")} từ data map → ép kiểu thủ công</li>
 *   <li>Sau: {@code instanceof GetItemRequest req} (pattern matching Java 16+)</li>
 *   <li>Response: {@code Response.success(item)} với typed body thay vì
 *       đặt item vào data map ({@code response.put("item", item)})</li>
 * </ul>
 *
 * <p><b>Lưu ý:</b> Không yêu cầu đăng nhập (public data) — ai cũng xem được
 * thông tin sản phẩm.
 */
public class GetItemHandler implements RequestHandler {

    private final AuctionService auctionService;

    public GetItemHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * Tìm item theo ID, trả full object qua {@code Response.success(item)}.
     * Item là subclass (Electronics/Art/Vehicle) — Serializable đa hình.
     */
    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (!(request instanceof GetItemRequest req)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        Optional<Item> opt = auctionService.getItem(req.getItemId());
        if (opt.isEmpty()) return HandlerUtils.error("Không tìm thấy sản phẩm");

        return Response.success(opt.get());
    }
}
