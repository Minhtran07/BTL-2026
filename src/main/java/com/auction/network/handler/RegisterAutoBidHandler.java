package com.auction.network.handler;

import com.auction.exception.InvalidBidException;
import com.auction.model.user.User;
import com.auction.network.Message;
import com.auction.service.AuctionService;

/**
 * ============================================================================
 * REGISTERAUTOBIDHANDLER - ĐĂNG KÝ AUTO-BID (PROXY BIDDING)
 * ============================================================================
 *
 * <p>Bidder đăng ký maxBid + increment. Server sẽ tự bid hộ user khi:
 * <ul>
 *   <li>Có bid mới vượt user → server tự bid (currentBid + increment)</li>
 *   <li>Dừng khi vượt maxBid của user</li>
 * </ul>
 *
 * <p>Sau khi đăng ký, AuctionService.registerAutoBid() sẽ kích hoạt ngay
 * processAutoBids() nếu user chưa dẫn đầu → có thể tạo nhiều bid mới ngay
 * trong 1 request này.
 */
public class RegisterAutoBidHandler implements RequestHandler {

    private final AuctionService auctionService;

    public RegisterAutoBidHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) return HandlerUtils.error("Chưa đăng nhập");

        try {
            auctionService.registerAutoBid(
                    request.get("auctionId"),
                    authenticatedUser.getId(),
                    authenticatedUser.getFullName(),
                    Double.parseDouble(request.get("maxBid")),
                    Double.parseDouble(request.get("increment")));

            Message response = new Message(Message.Type.SUCCESS);
            response.put("message", "Auto-Bid đã kích hoạt");
            return response;
        } catch (InvalidBidException e) {
            return HandlerUtils.error(e.getMessage());
        }
    }
}
