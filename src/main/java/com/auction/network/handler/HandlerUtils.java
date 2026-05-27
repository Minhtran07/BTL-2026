package com.auction.network.handler;

import com.auction.network.message.Message;

/**
 * ============================================================================
 * HANDLERUTILS - TIỆN ÍCH DÙNG CHUNG CHO MỌI HANDLER
 * ============================================================================
 *
 * <p>Class utility (chỉ chứa static method) - cung cấp các helper để tạo
 * response chuẩn cho client.
 *
 * <p><b>final + private constructor:</b> không cho phép khởi tạo (vì utility class
 * không có state, tất cả method đều static).
 */
public final class HandlerUtils {

    /** Private constructor - chống tạo instance. */
    private HandlerUtils() {}

    /**
     * Tạo response ERROR với message lỗi.
     *
     * @param message thông điệp lỗi (tiếng Việt) sẽ được hiển thị cho user
     * @return Message ERROR với key "error" chứa message
     */
    public static Message error(String message) {
        Message response = new Message(Message.Type.ERROR);
        response.put("error", message);
        return response;
    }

    /**
     * Tạo response thông báo "Chưa đăng nhập".
     * Dùng khi handler yêu cầu user phải login trước.
     */
    public static Message requireLogin(Message.Type type) {
        return error("Chưa đăng nhập");
    }
}
