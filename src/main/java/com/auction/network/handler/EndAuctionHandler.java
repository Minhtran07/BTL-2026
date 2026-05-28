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
 * <p>Phiên chuyển RUNNING → FINISHED, đồng thời thực hiện SETTLEMENT.
 */
public class EndAuctionHandler implements RequestHandler {

    private final AuctionService auctionService;

    public EndAuctionHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

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
