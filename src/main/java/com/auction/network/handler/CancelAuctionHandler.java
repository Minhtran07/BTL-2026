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
 * <p>Chuyển status → CANCELED. KHÔNG settlement.
 */
public class CancelAuctionHandler implements RequestHandler {

    private final AuctionService auctionService;

    public CancelAuctionHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

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
