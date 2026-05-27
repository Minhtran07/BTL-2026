package com.auction.network.handler;

import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.service.AuctionService;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * GETALLITEMSHANDLER - LẤY TẤT CẢ ITEM TRONG HỆ THỐNG
 * ============================================================================
 *
 * <p>Trả về List&lt;Item&gt; qua body + size qua data["count"].
 *
 * <p><b>Cast (Serializable) cần thiết:</b> Mặc dù ArrayList implements
 * Serializable, signature của setBody() yêu cầu Serializable nên phải cast
 * tường minh.
 */
public class GetAllItemsHandler implements RequestHandler {

    private final AuctionService auctionService;

    public GetAllItemsHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        // Copy sang ArrayList (đảm bảo Serializable)
        List<Item> items = new ArrayList<>(auctionService.getAllItems());
        Response response = Response.success();
        response.put("count", String.valueOf(items.size()));
        response.setBody((java.io.Serializable) items);
        return response;
    }
}
