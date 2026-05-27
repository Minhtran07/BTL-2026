package com.auction.network.handler;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
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
        if (!(request instanceof PlaceBidRequest bidReq)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        try {
            auctionService.placeBid(
                    bidReq.getAuctionId(),
                    authenticatedUser.getId(),
                    authenticatedUser.getFullName(),
                    bidReq.getAmount());

            return Response.success();
        } catch (InvalidBidException | AuctionClosedException e) {
            return Response.error(e.getMessage());
        }
    }
}
