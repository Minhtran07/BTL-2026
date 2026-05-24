package com.auction.pattern.factory;

import com.auction.model.item.Electronics;
import com.auction.model.item.Item;

import java.util.Map;

/**
 * Factory Method Pattern - <b>ConcreteCreator</b> cho danh mục
 * {@link com.auction.model.item.ItemCategory#ELECTRONICS ELECTRONICS}.
 *
 * <h3>Vai trò</h3>
 * Implement {@link ItemCreator} để biến {@code Map<String, String> extra} thành
 * một instance {@link Electronics} hoàn chỉnh.
 *
 * <h3>Các key đọc từ {@code extra}</h3>
 * <ul>
 *   <li><b>{@code brand}</b> — hãng sản xuất (vd Apple, Samsung)
 *       (default {@code "Unknown"}).</li>
 *   <li><b>{@code model}</b> — model cụ thể (vd iPhone 15)
 *       (default {@code "Unknown"}).</li>
 *   <li><b>{@code condition}</b> — tình trạng (vd {@code "NEW"}, {@code "USED"},
 *       {@code "REFURBISHED"}) (default {@code "USED"} — giả định an toàn cho
 *       đa số sản phẩm đấu giá).</li>
 * </ul>
 *
 * <h3>Lý do tách creator riêng</h3>
 * Electronics khác Art ở chỗ không có field numeric (không cần parseInt),
 * và default {@code condition} mang ý nghĩa nghiệp vụ riêng. Tách class riêng
 * cho phép thay đổi logic mà không ảnh hưởng creator khác (SRP).
 */
public class  ElectronicsCreator implements ItemCreator {

    /**
     * Tạo {@link Electronics} từ thông tin cơ bản + map thuộc tính mở rộng.
     *
     * <p>Tất cả field đều có default fallback "Unknown"/"USED" để creator không
     * bao giờ ném exception vì thiếu key — tầng caller (controller) không cần
     * validate {@code extra} trước.
     *
     * @param name     tên sản phẩm
     * @param desc     mô tả
     * @param price    giá khởi điểm
     * @param sellerId ID người bán
     * @param extra    map thuộc tính (xem class-level Javadoc cho các key)
     * @return instance {@link Electronics} đã khởi tạo
     */
    @Override
    public Item create(String name, String desc, double price,
                       String sellerId, Map<String, String> extra) {
        // Default "USED" thay vì "NEW": an toàn hơn về mặt pháp lý — nếu seller
        // không khai báo, hệ thống không tự gán nhãn "mới" gây hiểu nhầm.
        String brand     = extra.getOrDefault("brand",     "Unknown");
        String model     = extra.getOrDefault("model",     "Unknown");
        String condition = extra.getOrDefault("condition", "USED");
        return new Electronics(name, desc, price, sellerId, brand, model, condition);
    }
}
