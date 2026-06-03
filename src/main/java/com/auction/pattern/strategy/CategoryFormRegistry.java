package com.auction.pattern.strategy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * ============================================================================
 * REGISTRY - ĐĂNG KÝ VÀ TRA CỨU STRATEGY THEO DANH MỤC
 * ============================================================================
 *
 * <p>Tách riêng khỏi {@link CategoryFormStrategy} để tuân thủ:
 * <ul>
 *   <li><b>SRP</b> — Registry chỉ lo quản lý ánh xạ tên → strategy,
 *       không lẫn vào contract của strategy</li>
 *   <li><b>DIP</b> — Registry lưu {@code Supplier<CategoryFormStrategy>}
 *       (abstraction), không import trực tiếp các class cụ thể trong
 *       interface. Các strategy tự đăng ký mình vào đây</li>
 * </ul>
 *
 * <p><b>Cách dùng:</b>
 * <pre>
 *   // Lấy strategy theo tên
 *   CategoryFormStrategy strategy = CategoryFormRegistry.of("Điện tử");
 *
 *   // Đăng ký category mới
 *   CategoryFormRegistry.register("Đồ cổ", AntiqueFormStrategy::new);
 *
 *   // Lấy danh sách tên để đổ vào ComboBox
 *   List&lt;String&gt; names = CategoryFormRegistry.getRegisteredNames();
 * </pre>
 */
public final class CategoryFormRegistry {

    /**
     * Map lưu tên hiển thị → supplier tạo strategy.
     * Dùng LinkedHashMap để giữ thứ tự đăng ký (hiển thị trên ComboBox).
     */
    private static final Map<String, Supplier<CategoryFormStrategy>> REGISTRY = new LinkedHashMap<>();

    // Đăng ký 3 danh mục mặc định
    static {
        register("Điện tử", ElectronicsFormStrategy::new);
        register("Nghệ thuật", ArtFormStrategy::new);
        register("Phương tiện", VehicleFormStrategy::new);
    }

    private CategoryFormRegistry() {
        // Utility class — không cho tạo instance
    }

    /**
     * Đăng ký strategy mới vào registry.
     * Tuân thủ OCP: mở rộng bằng cách gọi method này, không sửa code cũ.
     *
     * @param displayName tên hiển thị tiếng Việt (key trên ComboBox)
     * @param supplier    factory tạo strategy instance
     */
    public static void register(String displayName, Supplier<CategoryFormStrategy> supplier) {
        REGISTRY.put(displayName, supplier);
    }

    /**
     * Trả về strategy phù hợp theo tên hiển thị category.
     * Tra cứu O(1) từ Map — không dùng if-else hay switch-case.
     *
     * @param displayName tên tiếng Việt ("Điện tử", "Nghệ thuật", "Phương tiện")
     * @return strategy tương ứng
     * @throws IllegalArgumentException nếu tên chưa được đăng ký
     */
    public static CategoryFormStrategy of(String displayName) {
        Supplier<CategoryFormStrategy> supplier = REGISTRY.get(displayName);
        if (supplier == null) {
            throw new IllegalArgumentException("Không có strategy cho danh mục: " + displayName);
        }
        return supplier.get();
    }

    /**
     * Trả về danh sách tên hiển thị đã đăng ký (giữ đúng thứ tự đăng ký).
     *
     * @return bản sao danh sách tên — sửa list này không ảnh hưởng registry
     */
    public static List<String> getRegisteredNames() {
        return new ArrayList<>(REGISTRY.keySet());
    }
}
