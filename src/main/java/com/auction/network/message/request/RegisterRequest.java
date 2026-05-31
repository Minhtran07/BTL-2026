package com.auction.network.message.request;

/**
 * Request đăng ký tài khoản — chứa thông tin user mới.
 * Handler: {@link com.auction.network.handler.RegisterHandler}
 * <p>{@code role} là "BUYER" hoặc "SELLER" (ADMIN bị chặn ở handler).
 */
public class RegisterRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String password;
    private final String email;
    private final String fullName;
    private final String role;

    public RegisterRequest(String username, String password,
                           String email, String fullName, String role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
}
