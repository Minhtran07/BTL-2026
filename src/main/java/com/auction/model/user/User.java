package com.auction.model.user;


import com.auction.model.entity.Entity;
import com.auction.util.PasswordUtils;

/**
 * Lớp trừu tượng User - kế thừa Entity.
 * Áp dụng Inheritance và Encapsulation.
 */
public abstract class User extends Entity {

    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String email;
    private String fullName;
    private boolean active;

    protected User() {
        super();
        this.active = true;
    }

    protected User(String username, String password, String email, String fullName) {
        super();
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.active = true;
    }

    // Encapsulation - private fields with getters/setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        markUpdated();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        markUpdated();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        markUpdated();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
        markUpdated();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        markUpdated();
    }

    /**
     * Phương thức trừu tượng trả về vai trò người dùng - Polymorphism.
     */
    public abstract UserRole getRole();

    /**
     * Xác minh mật khẩu sử dụng PasswordUtils (SHA-256 + salt).
     *
     * <p>Hỗ trợ migration: nếu password chưa được băm (không chứa ":"),
     * fallback về so sánh trực tiếp để các tài khoản cũ vẫn đăng nhập được
     * trong giai đoạn chuyển đổi.
     */
    public boolean validatePassword(String inputPassword) {
        if (this.password == null || inputPassword == null) return false;
        // Password đã được băm theo định dạng "salt:hash"
        if (this.password.contains(":")) {
            return PasswordUtils.verify(inputPassword, this.password);
        }
        // Fallback plaintext (tài khoản cũ tạo trước khi áp dụng hashing)
        return this.password.equals(inputPassword);
    }

    @Override
    public String printInfo() {
        return String.format("User[id=%s, username=%s, role=%s, email=%s, fullName=%s]",
                getId(), username, getRole(), email, fullName);
    }
}