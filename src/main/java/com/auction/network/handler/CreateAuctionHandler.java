package com.auction.network.handler;

import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.service.AuctionService;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * CREATEAUCTIONHANDLER - TẠO PHIÊN ĐẤU GIÁ MỚI
 * ============================================================================
 *
 * <p>Client gửi itemId + durationMinutes + startingPrice. Handler:
 * <ol>
 *   <li>Tính startTime = now, endTime = now + duration phút</li>
 *   <li>Gọi AuctionService.createAuction()</li>
 *   <li>Trả auctionId</li>
 * </ol>
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

        int durationMinutes = Integer.parseInt(request.get("duration"));
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end   = start.plusMinutes(durationMinutes);

        Auction auction = auctionService.createAuction(
                request.get("itemId"),
                authenticatedUser.getId(),
                request.get("itemName"),
                Double.parseDouble(request.get("startingPrice")),
                start, end);

        Response response = Response.success();
        response.put("auctionId", auction.getId());
        return response;
    }
}
