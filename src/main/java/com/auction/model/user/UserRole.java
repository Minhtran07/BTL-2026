package com.auction.model.user;


/**
 * ============================================================================
 * ENUM USERROLE - DANH SÁCH CÁC VAI TRÒ TRONG HỆ THỐNG
 * ============================================================================
 *
 * <p>Enum (Enumeration) là kiểu liệt kê - định nghĩa một tập hợp các hằng số
 * cố định. Ở đây có 3 vai trò:
 * <ul>
 *   <li>{@code BIDDER} - Người đặt giá (mua)</li>
 *   <li>{@code SELLER} - Người bán</li>
 *   <li>{@code ADMIN} - Quản trị viên</li>
 * </ul>
 *
 * <p><b>Tại sao dùng enum thay vì String hay int?</b>
 * <ul>
 *   <li>An toàn kiểu (type-safe): không thể truyền String tùy ý</li>
 *   <li>Compiler kiểm tra lỗi gõ sai tại thời điểm biên dịch</li>
 *   <li>Mỗi giá trị có thể có thuộc tính/phương thức riêng (như displayName ở đây)</li>
 *   <li>Tự động có {@code valueOf()}, {@code values()}, {@code toString()}</li>
 * </ul>
 */
public enum UserRole {
    // Mỗi enum value có thể truyền tham số vào constructor enum
    BIDDER("Bidder"),
    SELLER("Seller"),
    ADMIN("Admin");

    /**
     * Tên hiển thị (display) - dùng cho UI (label, dropdown, thông báo).
     * Khác với name() chuẩn của enum (luôn UPPERCASE).
     */
    private final String displayName;

    /**
     * Constructor của enum - LUÔN private (mặc định), Java chỉ gọi 1 lần
     * cho mỗi giá trị enum khi class được load.
     */
    UserRole(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Trả về tên hiển thị thân thiện với người dùng.
     * Ví dụ: {@code UserRole.BIDDER.getDisplayName()} → "Bidder"
     */
    public String getDisplayName() {
        return displayName;
    }
}
