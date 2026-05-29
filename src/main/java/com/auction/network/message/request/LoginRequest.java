package com.auction.network.message.request;

import com.auction.network.message.Request;

/**
 * Request đăng nhập — chứa username + password.
 * Handler: {@link com.auction.network.handler.LoginHandler}
 * <p>Trước: {@code new Message(Type.LOGIN) + put("username",…)}. Sau: typed fields.
 */
public class LoginRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
