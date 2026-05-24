package com.auction.network.handler;

import com.auction.model.user.User;
import com.auction.network.Message;
import com.auction.service.AuctionService;

/**
 * ============================================================================
 * ENDAUCTIONHANDLER - KẾT THÚC PHIÊN ĐẤU GIÁ THỦ CÔNG
 * ============================================================================
 *
 * <p>Khi gọi: phiên chuyển RUNNING → FINISHED, đồng thời thực hiện
 * SETTLEMENT (trừ tiền winner + cộng tiền seller).
 *
 * <p>Thường được gọi tự động bởi server scheduler khi hết giờ, nhưng cũng
 * có thể gọi thủ công (vd: admin force kết thúc).
 */
public class EndAuctionHandler implements RequestHandler {

    private final AuctionService auctionService;

    public EndAuctionHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) return HandlerUtils.error("Chưa đăng nhập");

        auctionService.endAuction(request.get("auctionId"));

        Message response = new Message(Message.Type.SUCCESS);
        response.put("message", "Phiên đấu giá đã kết thúc");
        return response;
    }
}
