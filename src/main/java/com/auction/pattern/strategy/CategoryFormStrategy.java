package com.auction.pattern.strategy;

import com.auction.model.item.ItemCategory;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * ============================================================================
 * STRATEGY PATTERN - CHIẾN LƯỢC TẠO FORM THEO DANH MỤC SẢN PHẨM
 * ============================================================================
 *
 * <p>Mỗi danh mục sản phẩm (Electronics, Art, Vehicle) cần các trường nhập
 * liệu khác nhau khi tạo phiên đấu giá. Strategy Pattern giúp tách logic
 * tạo UI và thu thập dữ liệu ra khỏi controller, tránh if-else dài.
 *
 * <p><b>SOLID:</b>
 * <ul>
 *   <li><b>SRP</b> — Interface chỉ định nghĩa contract cho strategy,
 *       không chứa registry hay UI helper</li>
 *   <li><b>OCP</b> — Thêm danh mục mới = tạo class mới + đăng ký vào
 *       {@link CategoryFormRegistry}, không sửa code cũ</li>
 *   <li><b>LSP</b> — Mọi implementation đều thay thế được cho nhau</li>
 *   <li><b>ISP</b> — Interface gọn, chỉ 3 method liên quan chặt chẽ</li>
 *   <li><b>DIP</b> — Controller phụ thuộc interface này, không biết
 *       class cụ thể nào implement</li>
 * </ul>
 *
 * @see CategoryFormRegistry
 * @see ElectronicsFormStrategy
 * @see ArtFormStrategy
 * @see VehicleFormStrategy
 */
public interface CategoryFormStrategy {

    /**
     * Trả về enum {@link ItemCategory} tương ứng với strategy này.
     */
    ItemCategory getCategory();

    /**
     * Tạo các trường nhập liệu động và thêm vào container.
     *
     * @param container VBox chứa các field động trên form
     */
    void buildFields(VBox container);

    /**
     * Thu thập dữ liệu từ các field đã tạo thành Map key-value.
     *
     * @return map chứa dữ liệu bổ sung (brand, artist, make...)
     */
    Map<String, String> collectData();
}
