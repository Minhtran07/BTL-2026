package com.auction.network.handler;

import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.service.AuctionService;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * ============================================================================
 * GETALLITEMSHANDLER - LẤY TẤT CẢ ITEM TRONG HỆ THỐNG
 * ============================================================================
 *
 * <p>Trả về {@code ArrayList<Item>} qua body của Response generic.
 */
public class GetAllItemsHandler implements RequestHandler {

    private final AuctionService auctionService;

    public GetAllItemsHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        return Response.success((Serializable) auctionService.getAllItems());
    }
}
