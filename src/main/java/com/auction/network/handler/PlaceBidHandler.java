package com.auction.network.handler;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.transaction.BidTransaction;
import com.auction.model.user.User;
import com.auction.network.Message;
import com.auction.service.AuctionService;

/**
 * ============================================================================
 * PLACEBIDHANDLER - XỬ LÝ REQUEST ĐẶT GIÁ
 * ============================================================================
 *
 * <p>Đây là handler quan trọng nhất - xử lý mỗi lần user đặt bid.
 *
 * <p>Quy trình:
 * <ol>
 *   <li>Kiểm tra user đã login</li>
 *   <li>Parse auctionId + amount từ request</li>
 *   <li>Gọi AuctionService.placeBid() - thực hiện validate + bid + auto-bid</li>
 *   <li>Trả SUCCESS với số tiền đã bid</li>
 * </ol>
 *
 * <p><b>KHÔNG TỰ BROADCAST:</b> Handler này KHÔNG gửi BID_UPDATE cho các client.
 * AuctionService đã dispatch event {@code NEW_BID} qua {@code AuctionEventDispatcher}.
 * Các ClientHandler đang subscribe phiên đó (Observer Pattern) tự nhận callback
 * → tự đẩy BID_UPDATE về client của mình.
 *
 * <p>Tách trách nhiệm như vậy giúp:
 * <ul>
 *   <li>PlaceBidHandler chỉ lo xử lý logic bid</li>
 *   <li>Push event được Observer Pattern xử lý ở chỗ khác</li>
 *   <li>Tuân thủ Single Responsibility Principle</li>
 * </ul>
 */
public class PlaceBidHandler implements RequestHandler {

    private final AuctionService auctionService;

    public PlaceBidHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Message handle(Message request, User authenticatedUser) {
        // Bắt buộc phải login mới được bid
        if (authenticatedUser == null) return HandlerUtils.error("Chưa đăng nhập");

        try {
            // Gọi service đặt bid (trả về BidTransaction nếu thành công)
            BidTransaction tx = auctionService.placeBid(
                    request.get("auctionId"),
                    authenticatedUser.getId(),
                    authenticatedUser.getFullName(),
                    Double.parseDouble(request.get("amount")));

            // Trả SUCCESS với info bid vừa thực hiện
            Message response = new Message(Message.Type.SUCCESS);
            response.put("bidAmount", String.valueOf(tx.getBidAmount()));
            response.put("message",   "Đặt giá thành công");
            return response;
        } catch (InvalidBidException | AuctionClosedException e) {
            // Bid không hợp lệ (giá thấp, phiên đã đóng...) → trả ERROR
            return HandlerUtils.error(e.getMessage());
        }
    }
}
