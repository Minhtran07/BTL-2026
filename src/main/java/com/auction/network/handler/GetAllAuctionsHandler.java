package com.auction.network.handler;

import com.auction.dao.AuctionDaoImpl;
import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.pattern.singleton.AuctionManager;
import com.auction.service.AuctionService;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * GETALLAUCTIONSHANDLER - LẤY TẤT CẢ PHIÊN ĐẤU GIÁ
 * ============================================================================
 *
 * <p><b>QUAN TRỌNG:</b> Đọc TRỰC TIẾP từ DAO (DB) thay vì AuctionManager.
 * Tại sao? AuctionManager là cache in-memory, có thể chưa load hết auction
 * từ DB. Đọc thẳng DB → đảm bảo client luôn thấy data đầy đủ và mới nhất.
 *
 * <p>Sau khi load, sync ngược lại vào AuctionManager để các thao tác sau
 * (placeBid, endAuction...) có thể tìm thấy auction trong cache.
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
        // Ưu tiên đọc thẳng từ DB để client luôn thấy auction mới nhất
        List<Auction> auctions = new ArrayList<>(auctionDao.findAll());
        // Đồng bộ vào AuctionManager để các thao tác sau (placeBid…) thấy được
        for (Auction a : auctions) {
            AuctionManager.getInstance().addAuction(a);
        }

        Message response = new Message(Message.Type.SUCCESS);
        response.put("count", String.valueOf(auctions.size()));
        response.setBody((java.io.Serializable) auctions);
        return response;
    }
}
