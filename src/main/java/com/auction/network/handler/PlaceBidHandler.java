package com.auction.network.handler;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.transaction.BidTransaction;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.PlaceBidRequest;
import com.auction.service.AuctionService;

public class PlaceBidHandler implements RequestHandler {

    private final AuctionService auctionService;

    public PlaceBidHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) return Response.error("Chưa đăng nhập");

        String auctionId;
        double amount;

        if (request instanceof PlaceBidRequest bidReq) {
            auctionId = bidReq.getAuctionId();
            amount = bidReq.getAmount();
        } else {
            auctionId = request.get("auctionId");
            amount = Double.parseDouble(request.get("amount"));
        }

        try {
            BidTransaction tx = auctionService.placeBid(
                    auctionId,
                    authenticatedUser.getId(),
                    authenticatedUser.getFullName(),
                    amount);

            Response response = Response.success();
            response.put("bidAmount", String.valueOf(tx.getBidAmount()));
            response.put("message", "Đặt giá thành công");
            return response;
        } catch (InvalidBidException | AuctionClosedException e) {
            return Response.error(e.getMessage());
        }
    }
}
