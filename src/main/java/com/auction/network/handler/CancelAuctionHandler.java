package com.auction.network.handler;

import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.CancelAuctionRequest;
import com.auction.service.AuctionService;

/**
 * ============================================================================
 * CANCELAUCTIONHANDLER - HỦY PHIÊN ĐẤU GIÁ
 * ============================================================================
 *
 * <p>Chuyển status → CANCELED. KHÔNG thực hiện settlement (không có người thắng).
 *
 * <p><b>Khác với EndAuction:</b>
 * <ul>
 *   <li>EndAuction: RUNNING → FINISHED + settlement (xác định winner)</li>
 *   <li>CancelAuction: RUNNING/OPEN → CANCELED, mọi bid bị vô hiệu</li>
 * </ul>
 *
 * <p><b>Refactoring:</b> Dùng {@code instanceof CancelAuctionRequest req}
 * (pattern matching Java 16+) thay cho {@code request.get("auctionId")} từ
 * data map chung. Trả {@code Response.success()} thay vì tạo Message thủ công.
 *
 * <p><b>Cache-Aside:</b> Service tự resolve auction từ cache hoặc DB.
 */
public class CancelAuctionHandler implements RequestHandler {

    private final AuctionService auctionService;

    public CancelAuctionHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * Hủy phiên đấu giá theo ID.
     * Pattern matching: {@code instanceof CancelAuctionRequest req}.
     */
    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) return HandlerUtils.error("Chưa đăng nhập");
        if (!(request instanceof CancelAuctionRequest req)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        auctionService.cancelAuction(req.getAuctionId());
        return Response.success();
    }
}
