package com.auction.network.handler;

import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.service.AuctionService;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * GETACTIVEAUCTIONSHANDLER - LẤY CHỈ CÁC PHIÊN ĐANG CHẠY
 * ============================================================================
 *
 * <p>Trả về List&lt;Auction&gt; chỉ chứa các phiên status = RUNNING.
 * Dùng cho màn hình chính (Bidder duyệt phiên đang nhận bid).
 *
 * <p>Khác GetAllAuctions: đọc từ AuctionManager (cache) → nhanh hơn,
 * dùng được vì các phiên RUNNING chắc chắn đã có trong cache.
 */
public class GetActiveAuctionsHandler implements RequestHandler {

    private final AuctionService auctionService;

    public GetActiveAuctionsHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        List<Auction> auctions = new ArrayList<>(auctionService.getActiveAuctions());
        Response response = Response.success();
        response.put("count", String.valueOf(auctions.size()));
        response.setBody((java.io.Serializable) auctions);
        return response;
    }
}
