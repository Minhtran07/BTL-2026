package com.auction.network.handler;

import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.CreateAuctionRequest;
import com.auction.service.AuctionService;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * CREATEAUCTIONHANDLER - TẠO PHIÊN ĐẤU GIÁ MỚI
 * ============================================================================
 *
 * <p>Client gửi {@link CreateAuctionRequest} chứa itemId, itemName,
 * startingPrice, durationMinutes. Handler tính thời gian rồi gọi service.
 *
 * <p><b>Lưu ý:</b> sellerId được lấy TỪ authenticatedUser (server tin user
 * hiện tại), không cho client tự truyền sellerId - tránh giả mạo.
 */
public class CreateAuctionHandler implements RequestHandler {

    private final AuctionService auctionService;

    public CreateAuctionHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) return HandlerUtils.error("Chưa đăng nhập");
        if (!(request instanceof CreateAuctionRequest req)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end   = start.plusMinutes(req.getDurationMinutes());

        Auction auction = auctionService.createAuction(
                req.getItemId(),
                authenticatedUser.getId(),
                req.getItemName(),
                req.getStartingPrice(),
                start, end);

        return Response.success(auction.getId());
    }
}
