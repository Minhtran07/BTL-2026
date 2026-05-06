package com.auction.pattern.factory;

import com.auction.model.item.Item;

import java.util.Map;

/**
 * Interface cho Factory Method Pattern (SOLID-compliant).
 *
 * <p>Nguyên tắc áp dụng:
 * <ul>
 *   <li>SRP – Mỗi creator chỉ chịu trách nhiệm tạo một loại Item.</li>
 *   <li>OCP – Thêm loại Item mới bằng cách tạo class mới, không sửa ItemFactory.</li>
 *   <li>DIP – ItemFactory phụ thuộc vào abstraction này, không phụ thuộc concrete class.</li>
 * </ul>
 */
public interface ItemCreator {

    /**
     * Tạo một Item từ thông tin cơ bản và các thuộc tính mở rộng.
     *
     * @param name     tên sản phẩm
     * @param desc     mô tả sản phẩm
     * @param price    giá khởi điểm
     * @param sellerId ID người bán
     * @param extra    thuộc tính bổ sung tuỳ loại (brand, model, artist, ...)
     * @return Item đã khởi tạo
     */
    Item create(String name, String desc, double price, String sellerId, Map<String, String> extra);
}
