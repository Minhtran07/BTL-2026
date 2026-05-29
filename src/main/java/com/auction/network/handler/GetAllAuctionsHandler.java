package com.auction.network.handler;

import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.service.AuctionService;

import java.io.Serializable;

/**
 * ============================================================================
 * GETALLAUCTIONSHANDLER - LẤY TẤT CẢ PHIÊN ĐẤU GIÁ
 * ============================================================================
 *
 * <p>Delegate hoàn toàn cho {@link AuctionService#getAllAuctions()} — service
 * tự ưu tiên bản cache cho phiên active (mới hơn DB) và chỉ lấy từ DB cho
 * phiên đã kết thúc/huỷ.
 */
public class GetAllAuctionsHandler implements RequestHandler {

    private final AuctionService auctionService;

    public GetAllAuctionsHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        return Response.success((Serializable) auctionService.getAllAuctions());
    }
}
