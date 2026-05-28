package com.auction.network.message.request;

import com.auction.network.message.Request;

public class DeactivateUserRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String userId;

    public DeactivateUserRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() { return userId; }
}
