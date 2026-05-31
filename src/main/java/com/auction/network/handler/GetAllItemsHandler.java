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
 * Không yêu cầu đăng nhập (public data).
 *
 * <p><b>Refactoring:</b>
 * <ul>
 *   <li>Trước: đặt list vào data map {@code response.put("items", list)}</li>
 *   <li>Sau: {@code Response.success((Serializable) list)} — body chứa
 *       trực tiếp, client dùng {@code response.getBody()} để lấy</li>
 * </ul>
 *
 * <p><b>Lưu ý:</b> Item là abstract class có 3 subtype (Electronics, Art,
 * Vehicle) — tất cả đều Serializable nên truyền qua socket OK.
 */
public class GetAllItemsHandler implements RequestHandler {

    private final AuctionService auctionService;

    public GetAllItemsHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /** Trả tất cả item. Body: {@code ArrayList<Item>}. Public — không cần login. */
    @Override
    public Message handle(Message request, User authenticatedUser) {
        return Response.success((Serializable) auctionService.getAllItems());
    }
}
