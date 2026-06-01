package com.auction.network.message.request;

/** Request khóa tài khoản user (chỉ Admin). Chứa userId cần khóa. */
public class DeactivateUserRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String userId;

    public DeactivateUserRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() { return userId; }
}
