package com.auction.network.message.request;

import com.auction.network.message.Request;

public class RegisterRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String password;
    private final String email;
    private final String fullName;
    private final String role;

    public RegisterRequest(String username, String password,
                           String email, String fullName, String role) {
        super(Type.REGISTER);
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        put("username", username);
        put("password", password);
        put("email", email);
        put("fullName", fullName);
        put("role", role);
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
}
