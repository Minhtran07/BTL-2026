package com.auction.pattern.factory;

import com.auction.model.item.Art;
import com.auction.model.item.Item;

import java.util.Map;

/**
 * Factory Method Pattern - <b>ConcreteCreator</b> cho danh mục
 * {@link com.auction.model.item.ItemCategory#ART ART}.
 *
 * <h3>Vai trò</h3>
 * Implement {@link ItemCreator} để biến {@code Map<String, String> extra} thành
 * một instance {@link Art} hoàn chỉnh.
 *
 * <h3>Các key đọc từ {@code extra}</h3>
 * <ul>
 *   <li><b>{@code artist}</b> — tên tác giả (default {@code "Unknown"}).</li>
 *   <li><b>{@code year}</b> — năm sáng tác, parse sang {@code int}
 *       (default {@code 2024} nếu null hoặc format sai).</li>
 *   <li><b>{@code medium}</b> — chất liệu (sơn dầu, màu nước, v.v.)
 *       (default {@code "Mixed"}).</li>
 * </ul>
 *
 * <h3>Lý do tách creator riêng</h3>
 * Mỗi loại Item có set thuộc tính khác nhau và logic parse khác nhau. Tách thành
 * class riêng giúp đạt SRP: nếu yêu cầu Art thay đổi (vd thêm trường
 * {@code certificate}), chỉ sửa class này, không ảnh hưởng creator khác.
 */
public class ArtCreator implements ItemCreator {

    /**
     * Tạo {@link Art} từ thông tin cơ bản + map thuộc tính mở rộng.
     *
     * <p>Mọi field đặc thù của Art đều có default fallback — tức là một map
     * {@code extra} rỗng vẫn tạo được Art hợp lệ (chỉ là dữ liệu sẽ là default).
     * Điều này tránh việc tầng UI/network phải validate đầy đủ trước khi gọi.
     *
     * @param name     tên tác phẩm
     * @param desc     mô tả
     * @param price    giá khởi điểm
     * @param sellerId ID người bán
     * @param extra    map thuộc tính (xem class-level Javadoc cho các key)
     * @return instance {@link Art} đã khởi tạo
     */
    @Override
    public Item create(String name, String desc, double price,
                       String sellerId, Map<String, String> extra) {
        // Fallback "Unknown"/"Mixed" để tránh null pointer ở tầng hiển thị
        String artist = extra.getOrDefault("artist", "Unknown");
        int    year   = parseIntOrDefault(extra.get("year"), 2024);
        String medium = extra.getOrDefault("medium", "Mixed");
        return new Art(name, desc, price, sellerId, artist, year, medium);
    }

    /**
     * Parse một String sang int, fallback về {@code defaultValue} khi null hoặc
     * format không hợp lệ.
     *
     * <p>Dùng để defensive-parse các giá trị numeric đến từ map (form input, JSON),
     * tránh ném {@link NumberFormatException} làm gãy luồng tạo Item.
     *
     * @param value        string cần parse (có thể null)
     * @param defaultValue giá trị trả về khi {@code value} null hoặc không parse được
     * @return giá trị int sau khi parse, hoặc {@code defaultValue}
     */
    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Format sai (vd "2024a", "abc") → coi như không có dữ liệu, dùng default
            return defaultValue;
        }
    }
}
