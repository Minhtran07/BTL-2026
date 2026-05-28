package com.auction.network.handler;

import com.auction.dao.AuctionDaoImpl;
import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.GetAuctionRequest;
import com.auction.pattern.singleton.AuctionManager;
import com.auction.service.AuctionService;

import java.util.Optional;

/**
 * ============================================================================
 * GETAUCTIONHANDLER - LẤY THÔNG TIN 1 PHIÊN ĐẤU GIÁ THEO ID
 * ============================================================================
 *
 * <p>Trả về full Auction object qua body. Đọc thẳng từ DAO để đảm bảo
 * data mới nhất, không bị stale do cache.
 */
public class GetAuctionHandler implements RequestHandler {

    @SuppressWarnings("unused")
    private final AuctionService auctionService;

    public GetAuctionHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (!(request instanceof GetAuctionRequest req)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        Optional<Auction> opt = auctionService.getFreshAuction(req.getAuctionId());
        if (opt.isEmpty()) return HandlerUtils.error("Không tìm thấy phiên đấu giá");

        Auction a = opt.get();
        AuctionManager.getInstance().addAuction(a);

        return Response.success(a);
    }
}
