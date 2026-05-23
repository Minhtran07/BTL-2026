package com.auction.model.user;

/**
 * ============================================================================
 * LỚP ADMIN - QUẢN TRỊ VIÊN HỆ THỐNG
 * ============================================================================
 *
 * <p>Admin có quyền cao nhất trong hệ thống:
 * <ul>
 *   <li>Vô hiệu hóa (deactivate) user bất kỳ - ngăn họ đăng nhập</li>
 *   <li>Xem toàn bộ phiên đấu giá, người dùng, sản phẩm</li>
 *   <li>Quản lý tổng quan hệ thống</li>
 * </ul>
 *
 * <p><b>Lưu ý:</b> Admin KHÔNG có balance hay listedItem - đơn giản chỉ là
 * một User với quyền quản trị. Không thể đăng ký Admin qua màn hình
 * Register thông thường - phải sửa DB trực tiếp hoặc dùng script khởi tạo.
 */
public class Admin extends User {

    private static final long serialVersionUID = 1L;

    /** Constructor mặc định - cho Java Serialization. */
    public Admin() {
        super();
    }

    /** Constructor đầy đủ - dùng khi tạo Admin (qua script seed DB). */
    public Admin(String username, String password, String email, String fullName) {
        super(username, password, email, fullName);
    }

    /** Polymorphism - trả về ADMIN. */
    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }

    @Override
    public String printInfo() {
        return String.format("Admin[id=%s, username=%s]", getId(), getUsername());
    }
}
