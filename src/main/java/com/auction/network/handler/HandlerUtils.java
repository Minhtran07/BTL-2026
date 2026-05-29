package com.auction.network.handler;

import com.auction.network.message.Message;
import com.auction.network.message.Response;

/**
 * ============================================================================
 * HANDLERUTILS - TIỆN ÍCH DÙNG CHUNG CHO HANDLER
 * ============================================================================
 *
 * <p>Utility class chứa helper methods cho tất cả handler. Hiện tại chỉ có
 * {@link #error(String)} — shortcut tạo {@code Response.error(message)}.
 *
 * <p><b>Tại sao cần?</b> Trước refactoring, mỗi handler tạo error response
 * bằng cách khác nhau (new Message(...), set data map...). Giờ tập trung
 * vào 1 chỗ, đảm bảo format nhất quán và dễ thay đổi sau này.
 *
 * <p><b>Thiết kế:</b> {@code final class} + private constructor → không thể
 * kế thừa hoặc tạo instance (pure utility class).
 */
public final class HandlerUtils {

    /** Ngăn tạo instance — utility class chỉ có static methods. */
    private HandlerUtils() {}

    /**
     * Tạo Response lỗi với message cho trước.
     * Shortcut cho {@code Response.error(message)} — dùng ở tất cả handler.
     */
    public static Message error(String message) {
        return Response.error(message);
    }
}
