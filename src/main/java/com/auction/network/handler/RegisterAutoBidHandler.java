package com.auction.network.handler;

import com.auction.exception.InvalidBidException;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.RegisterAutoBidRequest;
import com.auction.service.AuctionService;

/**
 * ============================================================================
 * REGISTERAUTOBIDHANDLER - ĐĂNG KÝ AUTO-BID (PROXY BIDDING)
 * ============================================================================
 *
 * <p>Bidder đăng ký maxBid + increment. Server sẽ tự bid hộ user khi
 * có bid mới vượt user, dừng khi vượt maxBid.
 */
public class RegisterAutoBidHandler implements RequestHandler {

    private final AuctionService auctionService;

    public RegisterAutoBidHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) return HandlerUtils.error("Chưa đăng nhập");
        if (authenticatedUser.getRole() == UserRole.ADMIN) {
            return HandlerUtils.error("Admin không được phép đặt giá");
        }
        if (!(request instanceof RegisterAutoBidRequest req)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        try {
            var auction = auctionService.getAuction(req.getAuctionId());
            if (auction.isPresent()
                    && auction.get().getSellerId().equals(authenticatedUser.getId())) {
                return HandlerUtils.error("Người bán không được tự đấu giá sản phẩm của mình");
            }

            auctionService.registerAutoBid(
                    req.getAuctionId(),
                    authenticatedUser.getId(),
                    authenticatedUser.getFullName(),
                    req.getMaxBid(),
                    req.getIncrement());

            return Response.success();
        } catch (InvalidBidException e) {
            return HandlerUtils.error(e.getMessage());
        }
    }
}
