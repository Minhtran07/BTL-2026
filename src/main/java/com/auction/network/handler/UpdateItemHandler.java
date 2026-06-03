package com.auction.network.handler;

import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.UpdateItemRequest;
import com.auction.service.AuctionService;

import java.util.List;

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
 *
 * <p><b>Refactoring:</b>
 * <ul>
 *   <li>Trước: đọc từng field từ data map, tự build Item object</li>
 *   <li>Sau: client gửi full Item object trong {@link UpdateItemRequest},
 *       dùng pattern matching {@code instanceof UpdateItemRequest req}</li>
 * </ul>
 *
 * <p><b>Authorization:</b> So sánh {@code item.getSellerId()} với
 * {@code authenticatedUser.getId()} — chỉ chủ sở hữu hoặc Admin mới sửa được.
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

        // Kiểm tra nếu giá thay đổi → chặn khi đã có bid
        var oldItem = auctionService.getItem(item.getId());
        boolean priceChanged = oldItem.isPresent()
                && oldItem.get().getStartingPrice() != item.getStartingPrice();

        List<Auction> relatedAuctions = auctionService.getAllAuctions().stream()
                .filter(a -> a.getItemId().equals(item.getId()))
                .toList();

        if (priceChanged) {
            boolean hasBids = relatedAuctions.stream().anyMatch(a -> a.getTotalBids() > 0);
            if (hasBids) {
                return HandlerUtils.error("Không thể sửa giá — đã có người đặt giá cho sản phẩm này");
            }
        }

        auctionService.updateItem(item);

        // Đồng bộ startingPrice sang auction (nếu giá thay đổi và chưa có bid)
        if (priceChanged) {
            for (Auction a : relatedAuctions) {
                a.setStartingPrice(item.getStartingPrice());
                a.setCurrentHighestBid(item.getStartingPrice());
                auctionService.updateAuction(a);
            }
        }

        return Response.success();
    }
}
