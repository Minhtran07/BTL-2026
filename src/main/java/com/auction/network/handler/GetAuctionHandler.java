package com.auction.network.handler;

import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.GetAuctionRequest;
import com.auction.service.AuctionService;

import java.util.Optional;

/**
 * ============================================================================
 * GETAUCTIONHANDLER - LẤY THÔNG TIN 1 PHIÊN ĐẤU GIÁ THEO ID
 * ============================================================================
 *
 * <p>Trả về full Auction object qua body. Ưu tiên đọc từ cache (RAM)
 * vì bản cache có state mới nhất (bid in-memory). Chỉ fallback sang DB
 * khi cache miss (phiên đã kết thúc hoặc server vừa restart).
 */
public class GetAuctionHandler implements RequestHandler {

    private final AuctionService auctionService;

    public GetAuctionHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (!(request instanceof GetAuctionRequest req)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        Optional<Auction> opt = auctionService.getAuction(req.getAuctionId());
        if (opt.isEmpty()) return HandlerUtils.error("Không tìm thấy phiên đấu giá");

        return Response.success(opt.get());
    }
}
