package com.auction.network.handler;

import com.auction.model.transaction.BidTransaction;
import com.auction.model.user.User;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.GetBidHistoryRequest;
import com.auction.service.AuctionService;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * ============================================================================
 * GETBIDHISTORYHANDLER - LẤY LỊCH SỬ BID CỦA 1 PHIÊN
 * ============================================================================
 *
 * <p>Trả về {@code ArrayList<BidTransaction>} qua body của Response.
 * Dùng cho AuctionDetailController hiển thị lịch sử bid + biểu đồ giá.
 *
 * <p><b>Refactoring:</b>
 * <ul>
 *   <li>Trước: {@code request.get("auctionId")} từ data map</li>
 *   <li>Sau: {@code instanceof GetBidHistoryRequest req} (Java 16+)</li>
 * </ul>
 *
 * <p><b>Cache-Aside:</b> Service tự resolve auction từ cache (nếu đang chạy)
 * hoặc DB (nếu đã kết thúc). Bid history trong cache luôn mới nhất cho
 * phiên RUNNING — không cần query DB riêng.
 */
public class GetBidHistoryHandler implements RequestHandler {

    private final AuctionService auctionService;

    public GetBidHistoryHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * Lấy lịch sử bid theo auctionId. Body: {@code ArrayList<BidTransaction>}.
     * Pattern matching: {@code instanceof GetBidHistoryRequest req}.
     */
    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (!(request instanceof GetBidHistoryRequest req)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        return Response.success((Serializable) auctionService.getBidHistory(req.getAuctionId()));
    }
}
