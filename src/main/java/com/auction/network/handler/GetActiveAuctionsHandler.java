package com.auction.network.handler;

import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.service.AuctionService;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * ============================================================================
 * GETACTIVEAUCTIONSHANDLER - LẤY CHỈ CÁC PHIÊN ĐANG CHẠY
 * ============================================================================
 *
 * <p>Trả về {@code ArrayList<Auction>} chỉ chứa các phiên status = RUNNING.
 * Đọc từ AuctionManager (cache) → nhanh hơn.
 */
public class GetActiveAuctionsHandler implements RequestHandler {

    private final AuctionService auctionService;

    public GetActiveAuctionsHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        ArrayList<Auction> auctions = new ArrayList<>(auctionService.getActiveAuctions());
        return Response.success((Serializable) auctions);
    }
}
