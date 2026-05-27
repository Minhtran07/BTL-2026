package com.auction.network.handler;

import com.auction.model.transaction.BidTransaction;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.service.AuctionService;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * GETBIDHISTORYHANDLER - LẤY LỊCH SỬ BID CỦA 1 PHIÊN
 * ============================================================================
 *
 * <p>Trả về List&lt;BidTransaction&gt; của 1 auction - dùng để hiển thị
 * biểu đồ giá theo thời gian và danh sách các lượt đặt giá.
 */
public class GetBidHistoryHandler implements RequestHandler {

    private final AuctionService auctionService;

    public GetBidHistoryHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        List<BidTransaction> history = new ArrayList<>(
                auctionService.getBidHistory(request.get("auctionId")));

        Response response = Response.success();
        response.put("count", String.valueOf(history.size()));
        response.setBody((java.io.Serializable) history);
        return response;
    }
}
