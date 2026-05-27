package com.auction.network.handler;

import com.auction.model.transaction.BidTransaction;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.GetBidHistoryRequest;
import com.auction.service.AuctionService;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * ============================================================================
 * GETBIDHISTORYHANDLER - LẤY LỊCH SỬ BID CỦA 1 PHIÊN
 * ============================================================================
 *
 * <p>Trả về {@code ArrayList<BidTransaction>} qua body của Response.
 */
public class GetBidHistoryHandler implements RequestHandler {

    private final AuctionService auctionService;

    public GetBidHistoryHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (!(request instanceof GetBidHistoryRequest req)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        ArrayList<BidTransaction> history = new ArrayList<>(
                auctionService.getBidHistory(req.getAuctionId()));

        return Response.success((Serializable) history);
    }
}
