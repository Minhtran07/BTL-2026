package com.auction.network.handler;

import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.EndAuctionRequest;
import com.auction.service.AuctionService;

/**
 * ============================================================================
 * ENDAUCTIONHANDLER - KẾT THÚC PHIÊN ĐẤU GIÁ THỦ CÔNG
 * ============================================================================
 *
 * <p>Phiên chuyển RUNNING → FINISHED, đồng thời thực hiện SETTLEMENT
 * (xác định người thắng, cập nhật DB).
 *
 * <p><b>Refactoring:</b>
 * <ul>
 *   <li>Trước: dùng {@code request.get("auctionId")} từ data map,
 *       trả về {@code new Message(Type.END_AUCTION_RESPONSE)}</li>
 *   <li>Sau: dùng {@code instanceof EndAuctionRequest req} (pattern matching
 *       Java 16+) để ép kiểu type-safe. Trả {@code Response.success()}</li>
 * </ul>
 *
 * <p><b>Service layer tự xử lý cache miss:</b> {@code endAuction()} gọi
 * {@code resolveAuction()} nội bộ — nếu auction chưa có trong RAM cache,
 * tự load từ DB lên (Cache-Aside pattern).
 */
public class EndAuctionHandler implements RequestHandler {

    private final AuctionService auctionService;

    public EndAuctionHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * Kết thúc phiên đấu giá theo ID.
     * Pattern matching: {@code instanceof EndAuctionRequest req}.
     */
    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) return HandlerUtils.error("Chưa đăng nhập");
        if (!(request instanceof EndAuctionRequest req)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        auctionService.endAuction(req.getAuctionId());
        return Response.success();
    }
}
