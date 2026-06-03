package com.auction.network.handler;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.message.Message;
import com.auction.network.message.Response;
import com.auction.network.message.request.PlaceBidRequest;
import com.auction.service.AuctionService;

/**
 * ============================================================================
 * PLACEBIDHANDLER - XỬ LÝ ĐẶT GIÁ (BID)
 * ============================================================================
 *
 * <p>Nhận {@link PlaceBidRequest} chứa auctionId + amount,
 * gọi {@link AuctionService#placeBid} để xác thực và ghi nhận bid.
 *
 * <p><b>Refactoring:</b> Trước đây dùng {@code request.get("auctionId")} từ
 * data map chung, ép kiểu thủ công. Giờ dùng {@code instanceof PlaceBidRequest bidReq}
 * (pattern matching Java 16+) → type-safe, không cần cast, compile-time check.
 *
 * <p><b>Service layer xử lý:</b>
 * <ul>
 *   <li>Validate: bid phải > giá hiện tại, auction phải đang RUNNING</li>
 *   <li>Anti-sniping: tự gia hạn nếu bid ở cuối phiên</li>
 *   <li>Auto-bid: nếu có người khác đăng ký auto-bid, service tự counter-bid</li>
 *   <li>Push notification: broadcast BidUpdatePush cho tất cả subscriber</li>
 * </ul>
 *
 * <p><b>Error handling:</b> Service ném {@link InvalidBidException} (giá không hợp lệ)
 * hoặc {@link AuctionClosedException} (phiên đã kết thúc) → handler bắt và trả
 * {@code Response.error(message)} với thông báo cụ thể.
 */
public class PlaceBidHandler implements RequestHandler {

    private final AuctionService auctionService;

    public PlaceBidHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * Xử lý bid request.
     *
     * <p>Pattern matching: {@code instanceof PlaceBidRequest bidReq} — kiểm tra
     * kiểu + ép kiểu + đặt tên biến trong 1 bước (Java 16+).
     *
     * <p>Response: {@code Response.success()} không body (client chỉ cần biết
     * bid thành công). Chi tiết giá mới sẽ đến qua push event.
     */
    @Override
    public Message handle(Message request, User authenticatedUser) {
        if (authenticatedUser == null) return Response.error("Chưa đăng nhập");
        if (authenticatedUser.getRole() == UserRole.ADMIN) {
            return HandlerUtils.error("Admin không được phép đặt giá");
        }
        if (!(request instanceof PlaceBidRequest bidReq)) {
            return HandlerUtils.error("Request không hợp lệ");
        }

        try {
            auctionService.placeBid(
                    bidReq.getAuctionId(),
                    authenticatedUser.getId(),
                    authenticatedUser.getFullName(),
                    bidReq.getAmount());

            return Response.success();  // Không cần body — push event sẽ thông báo giá mới
        } catch (InvalidBidException | AuctionClosedException e) {
            return Response.error(e.getMessage());
        }
    }
}
