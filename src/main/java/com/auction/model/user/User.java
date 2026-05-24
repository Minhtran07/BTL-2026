package com.auction.model.user;


import com.auction.model.entity.Entity;
import com.auction.util.PasswordUtils;

/**
 * ============================================================================
 * LỚP USER - LỚP TRỪU TƯỢNG ĐẠI DIỆN NGƯỜI DÙNG TRONG HỆ THỐNG
 * ============================================================================
 *
 * <p>Đây là lớp <b>abstract</b> (trừu tượng) - không thể tạo trực tiếp bằng
 * {@code new User(...)} mà phải tạo thông qua các lớp con cụ thể:
 * <ul>
 *   <li>{@link Bidder} - Người tham gia đấu giá (đặt giá)</li>
 *   <li>{@link Seller} - Người bán (đăng sản phẩm để đấu giá)</li>
 *   <li>{@link Admin} - Quản trị viên hệ thống</li>
 * </ul>
 *
 * <p><b>Áp dụng các nguyên tắc OOP:</b>
 * <ul>
 *   <li><b>Inheritance (kế thừa):</b> User kế thừa từ {@link Entity} để có
 *       id, createdAt, updatedAt sẵn.</li>
 *   <li><b>Encapsulation (bao đóng):</b> Tất cả field {@code private}, chỉ
 *       truy cập qua getter/setter để kiểm soát việc đọc/ghi.</li>
 *   <li><b>Polymorphism (đa hình):</b> Phương thức {@link #getRole()} là
 *       abstract → mỗi class con trả về role khác nhau.</li>
 *   <li><b>Abstraction (trừu tượng):</b> Khai báo User là abstract → khái
 *       quát "người dùng" mà không cần biết loại cụ thể.</li>
 * </ul>
 */
public abstract class User extends Entity {

    /**
     * serialVersionUID dùng cho Java Serialization.
     * Khi gửi đối tượng qua socket, Java cần kiểm tra phiên bản class có khớp
     * giữa 2 đầu (client/server) hay không. Khai báo cố định = 1L để tránh
     * lỗi InvalidClassException khi hai bên đã build code khớp nhau.
     */
    private static final long serialVersionUID = 1L;

    /** Tên đăng nhập - duy nhất trong hệ thống. Dùng để login. */
    private String username;

    /**
     * Mật khẩu đã được băm dạng "salt:hash" (PBKDF2-style).
     * KHÔNG BAO GIỜ lưu plaintext - chống lộ thông tin nếu DB bị hack.
     */
    private String password;

    /** Địa chỉ email của user (dùng để liên hệ, khôi phục tài khoản...). */
    private String email;

    /** Họ và tên đầy đủ của user. */
    private String fullName;

    /**
     * Trạng thái hoạt động.
     * {@code true} = bình thường, {@code false} = bị admin vô hiệu hóa
     * (không thể đăng nhập / tham gia).
     */
    private boolean active;

    /**
     * Constructor không tham số - cần thiết để Java Serialization khôi phục
     * đối tượng khi nhận qua socket. Mặc định active = true.
     */
    protected User() {
        super(); // gọi constructor của Entity để sinh id, createdAt
        this.active = true;
    }

    /**
     * Constructor đầy đủ - dùng khi tạo user mới (đăng ký).
     *
     * @param username tên đăng nhập (cần unique)
     * @param password mật khẩu (đã được băm trước khi gọi vào đây)
     * @param email    email liên hệ
     * @param fullName họ tên đầy đủ
     */
    protected User(String username, String password, String email, String fullName) {
        super();
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.active = true; // mặc định user mới luôn active
    }

    // ===== GETTER/SETTER - ÁP DỤNG ENCAPSULATION =====
    // Mọi setter đều gọi markUpdated() để cập nhật updatedAt -> audit trail

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        markUpdated(); // ghi nhận có thay đổi → updatedAt = now
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
     * Phương thức trừu tượng trả về vai trò của user.
     *
     * <p>Đây là điểm thể hiện <b>Polymorphism</b>: từ tham chiếu User,
     * gọi getRole() sẽ tự động chạy đúng version của lớp con (Bidder,
     * Seller hoặc Admin) - Java tự xác định tại runtime.
     *
     * @return enum {@link UserRole} tương ứng với loại user
     */
    public abstract UserRole getRole();

    /**
     * Xác minh mật khẩu nhập vào có khớp với mật khẩu lưu trong hệ thống.
     *
     * <p><b>Cách hoạt động:</b>
     * <ol>
     *   <li>Nếu password lưu có dấu ":" → đã được băm dạng "salt:hash" →
     *       dùng {@link PasswordUtils#verify(String, String)} để xác minh.</li>
     *   <li>Nếu không có dấu ":" → là plaintext cũ (legacy), so sánh trực tiếp.
     *       Đây là cơ chế migration - cho phép tài khoản cũ vẫn login được
     *       trong giai đoạn chuyển đổi.</li>
     * </ol>
     *
     * @param inputPassword mật khẩu plaintext user nhập vào
     * @return {@code true} nếu khớp, {@code false} nếu sai hoặc null
     */
    public boolean validatePassword(String inputPassword) {
        // Kiểm tra null trước để tránh NullPointerException
        if (this.password == null || inputPassword == null) {
            return false;
        }
        // Format "salt:hash" → đã được hash
        if (this.password.contains(":")) {
            return PasswordUtils.verify(inputPassword, this.password);
        }
        // Fallback cho tài khoản cũ (chưa hash) - chỉ giữ để backward compat
        return this.password.equals(inputPassword);
    }

    /**
     * Override phương thức abstract từ Entity - in thông tin tóm tắt user.
     * KHÔNG hiển thị password vì lý do bảo mật.
     */
    @Override
    public String printInfo() {
        return String.format("User[id=%s, username=%s, role=%s, email=%s, fullName=%s]",
                getId(), username, getRole(), email, fullName);
    }
}
