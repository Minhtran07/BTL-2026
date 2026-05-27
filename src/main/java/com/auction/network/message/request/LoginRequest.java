package com.auction.network.message.request;

import com.auction.network.message.Request;

public class LoginRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String password;

    public LoginRequest(String username, String password) {
        super(Type.LOGIN);
        this.username = username;
        this.password = password;
        put("username", username);
        put("password", password);
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
