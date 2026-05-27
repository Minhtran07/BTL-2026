package com.auction.network.handler;

import com.auction.model.item.Item;
import com.auction.model.item.ItemCategory;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.service.AuctionService;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * CREATEITEMHANDLER - XỬ LÝ REQUEST TẠO SẢN PHẨM MỚI
 * ============================================================================
 *
 * <p>Quy trình:
 * <ol>
 *   <li>Kiểm tra đã login</li>
 *   <li>Parse category (Electronics/Art/Vehicle)</li>
 *   <li>Tách "extra fields" ra khỏi các field chung (loại key bằng RESERVED_KEYS)</li>
 *   <li>Gọi AuctionService.createItem() - dùng Factory Pattern tạo đúng subclass</li>
 *   <li>Trả về itemId cho client (để dùng tiếp tạo Auction)</li>
 * </ol>
 *
 * <p><b>RESERVED_KEYS:</b> Các key đã được dùng cho field chung của Item
 * (category, name, description, price). Các key KHÁC trong request data
 * được xem là field riêng của subtype (vd: brand, model, artist...) →
 * gộp vào Map extra để truyền vào Factory.
 */
public class CreateItemHandler implements RequestHandler {

    /** Các key dành riêng cho field chung, không phải extra field của subtype. */
    private static final java.util.Set<String> RESERVED_KEYS =
            java.util.Set.of("category", "name", "description", "price");

    private final AuctionService auctionService;

    public CreateItemHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) return HandlerUtils.error("Chưa đăng nhập");

        // Parse category
        ItemCategory category = ItemCategory.valueOf(request.get("category"));

        // Lọc ra các "extra fields" (brand, model, artist, mileage...) - không phải key chung
        Map<String, String> extra = new HashMap<>();
        for (Map.Entry<String, String> entry : request.getData().entrySet()) {
            if (!RESERVED_KEYS.contains(entry.getKey())) {
                extra.put(entry.getKey(), entry.getValue());
            }
        }

        // Gọi service tạo item (sẽ dùng Factory Pattern)
        Item item = auctionService.createItem(
                category,
                request.get("name"),
                request.get("description"),
                Double.parseDouble(request.get("price")),
                authenticatedUser.getId(), // sellerId = user đang login
                extra);

        // Trả về itemId mới
        Response response = Response.success();
        response.put("itemId", item.getId());
        return response;
    }
}
