package com.auction.network.handler;

import com.auction.network.message.Message;
import com.auction.network.message.Response;

public final class HandlerUtils {

    private HandlerUtils() {}

    public static Message error(String message) {
        return Response.error(message);
    }

    public static Message requireLogin(Message.Type type) {
        return Response.error("Chưa đăng nhập");
    }
}
