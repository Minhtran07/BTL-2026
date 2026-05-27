package com.auction.network.handler;

import com.auction.dao.AuctionDaoImpl;
import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.pattern.singleton.AuctionManager;
import com.auction.service.AuctionService;

import java.util.Optional;

/**
 * ============================================================================
 * GETAUCTIONHANDLER - LẤY THÔNG TIN 1 PHIÊN ĐẤU GIÁ THEO ID
 * ============================================================================
 *
 * <p>Trả về full Auction object qua body. Dùng khi client mở màn hình
 * chi tiết phiên đấu giá.
 *
 * <p>Đọc thẳng từ DAO (giống GetAllAuctions) để đảm bảo data mới nhất,
 * không bị stale do cache.
 */
public class GetAuctionHandler implements RequestHandler {

    @SuppressWarnings("unused")
    private final AuctionService auctionService;
    private final AuctionDaoImpl auctionDao = new AuctionDaoImpl();

    public GetAuctionHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        String id = request.get("auctionId");

        Optional<Auction> opt = auctionDao.findById(id);
        if (opt.isEmpty()) return HandlerUtils.error("Không tìm thấy phiên đấu giá");

        Auction a = opt.get();
        AuctionManager.getInstance().addAuction(a);

        Message response = new Message(Message.Type.SUCCESS);
        response.setBody(a);
        return response;
    }
}
