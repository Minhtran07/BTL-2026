package com.auction.pattern.factory;

import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;

import java.util.Map;

/**
 * ============================================================================
 * VEHICLECREATOR - CONCRETE CREATOR CHO VEHICLE (FACTORY METHOD PATTERN)
 * ============================================================================
 *
 * <p>Class này thực hiện Factory Method Pattern: nhận dữ liệu thô (Map) và
 * tạo ra đối tượng Vehicle phù hợp. Tuân thủ <b>SRP</b> (Single Responsibility):
 * chỉ chịu trách nhiệm khởi tạo Vehicle.
 *
 * <p><b>Các key cần có trong map "extra":</b>
 * <ul>
 *   <li>{@code make}: hãng xe (vd: "Toyota") - default "Unknown"</li>
 *   <li>{@code vehicleModel}: dòng xe (vd: "Camry") - default "Unknown"</li>
 *   <li>{@code year}: năm sản xuất (int) - default 2024</li>
 *   <li>{@code mileage}: số km đã đi - default 0</li>
 * </ul>
 *
 * <p><b>Tại sao tách thành class riêng?</b> Để tuân thủ Open/Closed Principle:
 * khi muốn thêm loại Item mới (vd: Jewelry), chỉ cần tạo JewelryCreator,
 * không cần sửa code các Creator hiện có.
 */
public class VehicleCreator implements ItemCreator {

    /**
     * Tạo đối tượng Vehicle từ thông tin truyền vào.
     * Dùng fallback default cho các key thiếu để tránh NullPointerException.
     */
    @Override
    public Item create(String name, String desc, double price,
                       String sellerId, Map<String, String> extra) {
        // Đọc các field với default fallback
        String make         = extra.getOrDefault("make",          "Unknown");
        String vehicleModel = extra.getOrDefault("vehicleModel",  "Unknown");
        // Parse số an toàn (không ném exception nếu format sai)
        int    year         = parseIntOrDefault(extra.get("year"),    2024);
        int    mileage      = parseIntOrDefault(extra.get("mileage"),    0);
        return new Vehicle(name, desc, price, sellerId, make, vehicleModel, year, mileage);
    }

    /**
     * Parse String → int một cách an toàn.
     * Nếu value null hoặc format sai → trả về defaultValue (không throw exception).
     *
     * @param value        String cần parse
     * @param defaultValue giá trị mặc định nếu parse thất bại
     * @return int sau khi parse, hoặc defaultValue
     */
    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Bắt NumberFormatException - không làm gãy luồng tạo Item
            return defaultValue;
        }
    }
}
