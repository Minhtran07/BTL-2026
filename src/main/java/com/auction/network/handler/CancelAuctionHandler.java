package com.auction.network.handler;

import com.auction.model.user.User;
import com.auction.network.Message;
import com.auction.service.AuctionService;

/**
 * ============================================================================
 * CANCELAUCTIONHANDLER - HỦY PHIÊN ĐẤU GIÁ
 * ============================================================================
 *
 * <p>Khi seller hủy phiên: chuyển status → CANCELED.
 * KHÔNG settlement (không có winner thực sự).
 *
 * <p>TODO: Nên kiểm tra quyền - chỉ seller của phiên hoặc admin được phép hủy.
 */
public class CancelAuctionHandler implements RequestHandler {

    private final AuctionService auctionService;

    public CancelAuctionHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) return HandlerUtils.error("Chưa đăng nhập");

        auctionService.cancelAuction(request.get("auctionId"));

        Message response = new Message(Message.Type.SUCCESS);
        response.put("message", "Phiên đấu giá đã hủy");
        return response;
    }
}
