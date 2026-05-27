package com.auction.network.handler;

import com.auction.dao.AuctionDaoImpl;
import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.pattern.singleton.AuctionManager;
import com.auction.service.AuctionService;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * ============================================================================
 * GETALLAUCTIONSHANDLER - LẤY TẤT CẢ PHIÊN ĐẤU GIÁ
 * ============================================================================
 *
 * <p>Đọc TRỰC TIẾP từ DAO (DB) thay vì AuctionManager để đảm bảo data
 * đầy đủ và mới nhất. Sau đó sync ngược vào AuctionManager.
 */
public class GetAllAuctionsHandler implements RequestHandler {

    @SuppressWarnings("unused")
    private final AuctionService auctionService;
    private final AuctionDaoImpl auctionDao = new AuctionDaoImpl();

    public GetAllAuctionsHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        ArrayList<Auction> auctions = new ArrayList<>(auctionDao.findAll());
        for (Auction a : auctions) {
            AuctionManager.getInstance().addAuction(a);
        }
        return Response.success((Serializable) auctions);
    }
}
