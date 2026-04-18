package com.auction.pattern.factory;

import com.auction.model.item.Item;
import com.auction.model.item.ItemCategory;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Factory đăng ký (Registry-based Factory Method) – SOLID compliant.
 *
 * <p>Các nguyên tắc được áp dụng:
 * <ul>
 *   <li><b>SRP</b> – Factory chỉ chịu trách nhiệm điều phối việc tạo Item thông
 *       qua registry. Logic khởi tạo cụ thể nằm ở từng {@link ItemCreator}.</li>
 *   <li><b>OCP</b> – Thêm loại Item mới bằng cách tạo {@link ItemCreator} mới và
 *       gọi {@link #register}, không cần sửa class này.</li>
 *   <li><b>LSP</b> – Mọi {@link ItemCreator} đều có thể thay thế nhau qua registry.</li>
 *   <li><b>DIP</b> – Phụ thuộc vào {@link ItemCreator} (abstraction), không phụ
 *       thuộc trực tiếp vào Electronics, Art, Vehicle.</li>
 * </ul>
 *
 * <p>Cách thêm loại mới (ví dụ {@code JEWELRY}):
 * <pre>{@code
 *   ItemFactory.register(ItemCategory.JEWELRY, new JewelryCreator());
 * }</pre>
 */
public class ItemFactory {

    private static final Map<ItemCategory, ItemCreator> REGISTRY =
            new EnumMap<>(ItemCategory.class);

    static {
        REGISTRY.put(ItemCategory.ELECTRONICS, new ElectronicsCreator());
        REGISTRY.put(ItemCategory.ART,         new ArtCreator());
        REGISTRY.put(ItemCategory.VEHICLE,     new VehicleCreator());
    }

    private ItemFactory() {
        // Utility class – không cho phép khởi tạo
    }

    /**
     * Đăng ký một {@link ItemCreator} cho một danh mục.
     * Hỗ trợ mở rộng không cần sửa factory (OCP).
     *
     * @param category danh mục sản phẩm
     * @param creator  đối tượng tạo Item tương ứng
     * @throws IllegalArgumentException nếu {@code category} hoặc {@code creator} là null
     */
    public static void register(ItemCategory category, ItemCreator creator) {
        if (category == null) throw new IllegalArgumentException("category must not be null");
        if (creator  == null) throw new IllegalArgumentException("creator must not be null");
        REGISTRY.put(category, creator);
    }

    /**
     * Tạo Item theo danh mục bằng cách tra cứu registry.
     *
     * @param category  loại sản phẩm
     * @param name      tên sản phẩm
     * @param desc      mô tả
     * @param price     giá khởi điểm
     * @param sellerId  ID người bán
     * @param extra     thuộc tính bổ sung tuỳ loại (brand, model, artist, ...)
     * @return Item tương ứng
     * @throws IllegalArgumentException nếu category chưa được đăng ký
     */
    public static Item createItem(ItemCategory category, String name, String desc,
                                  double price, String sellerId, Map<String, String> extra) {
        ItemCreator creator = REGISTRY.get(category);
        if (creator == null) {
            throw new IllegalArgumentException("No creator registered for category: " + category);
        }
        return creator.create(name, desc, price, sellerId, extra);
    }

    /**
     * Trả về danh sách các danh mục đang được hỗ trợ (read-only).
     */
    public static Map<ItemCategory, ItemCreator> getRegisteredCreators() {
        return Collections.unmodifiableMap(REGISTRY);
    }
}
