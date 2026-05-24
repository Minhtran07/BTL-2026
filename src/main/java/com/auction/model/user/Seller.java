package com.auction.model.user;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * LỚP SELLER - NGƯỜI BÁN HÀNG (NGƯỜI ĐĂNG SẢN PHẨM)
 * ============================================================================
 *
 * <p>Seller là loại User cụ thể có quyền:
 * <ul>
 *   <li>Tạo Item (sản phẩm) trên hệ thống</li>
 *   <li>Mở phiên đấu giá cho item của mình</li>
 *   <li>Hủy phiên đấu giá khi chưa có bid</li>
 * </ul>
 *
 * <p><b>Field riêng của Seller:</b>
 * <ul>
 *   <li>{@code listedItemIds}: danh sách ID các sản phẩm đang đăng</li>
 *   <li>{@code totalRevenue}: tổng doanh thu (cộng dồn từ các phiên thắng)</li>
 * </ul>
 */
public class Seller extends User {

    private static final long serialVersionUID = 1L;

    /**
     * Danh sách ID các sản phẩm đang đăng bán.
     * {@code final} bảo vệ tham chiếu list (không gán list khác lên field).
     */
    private final List<String> listedItemIds;

    /** Tổng doanh thu cộng dồn từ các phiên đấu giá thành công. */
    private double totalRevenue;

    /** Constructor không tham số - cho Java Serialization. */
    public Seller() {
        super();
        this.listedItemIds = new ArrayList<>();
        this.totalRevenue = 0.0;
    }

    /** Constructor đầy đủ - khi đăng ký Seller mới. */
    public Seller(String username, String password, String email, String fullName) {
        super(username, password, email, fullName);
        this.listedItemIds = new ArrayList<>();
        this.totalRevenue = 0.0;
    }

    /** Trả về SELLER → Polymorphism. */
    @Override
    public UserRole getRole() {
        return UserRole.SELLER;
    }

    /** Trả về bản sao của list (defensive copy - bảo vệ encapsulation). */
    public List<String> getListedItemIds() {
        return new ArrayList<>(listedItemIds);
    }

    /** Thêm item mới vào danh sách đăng bán. */
    public void addListedItem(String itemId) {
        listedItemIds.add(itemId);
        markUpdated();
    }

    /** Xóa item khỏi danh sách (khi seller xóa item hoặc bán xong). */
    public void removeListedItem(String itemId) {
        listedItemIds.remove(itemId);
        markUpdated();
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    /**
     * Cộng thêm doanh thu sau khi 1 phiên đấu giá kết thúc thành công.
     *
     * @param amount số tiền seller nhận được (giá thắng cuối cùng)
     */
    public void addRevenue(double amount) {
        this.totalRevenue += amount;
        markUpdated();
    }

    @Override
    public String printInfo() {
        return String.format("Seller[id=%s, username=%s, items=%d, revenue=%.2f]",
                getId(), getUsername(), listedItemIds.size(), totalRevenue);
    }
}
