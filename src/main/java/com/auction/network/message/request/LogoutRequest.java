package com.auction.network.message.request;

import com.auction.network.message.Request;

public class LogoutRequest extends Request {
    private static final long serialVersionUID = 1L;

    public LogoutRequest() {
        super(Type.LOGOUT);
    }
}
