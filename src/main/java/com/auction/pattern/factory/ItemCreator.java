package com.auction.pattern.factory;

import com.auction.model.item.Item;

import java.util.Map;

/**
 * Factory Method Pattern - <b>Creator</b> abstraction.
 *
 * <h3>Vai trò trong pattern</h3>
 * <ul>
 *   <li><b>Creator (abstraction)</b>: interface này — khai báo phương thức factory
 *       {@link #create(String, String, double, String, Map)} chung cho mọi loại Item.</li>
 *   <li><b>ConcreteCreator</b>: {@link ElectronicsCreator}, {@link ArtCreator},
 *       {@link VehicleCreator} — mỗi class biết cách map các key trong {@code extra}
 *       sang tham số khởi tạo của một concrete {@link Item} cụ thể.</li>
 *   <li><b>Product</b>: hierarchy {@link Item} với các subclass {@code Electronics},
 *       {@code Art}, {@code Vehicle}.</li>
 *   <li><b>Client</b>: {@link ItemFactory} — lookup creator theo {@code ItemCategory}
 *       trong registry rồi delegate sang {@link #create}.</li>
 * </ul>
 *
 * <h3>Lý do dùng Factory Method ở đây</h3>
 * <ul>
 *   <li>Service layer (controller / DAO) chỉ có dữ liệu dạng generic
 *       {@code Map<String, String>} (vd nhận từ form UI hoặc payload network).
 *       Mỗi loại Item cần các key khác nhau (Electronics cần {@code brand},
 *       Art cần {@code artist}, Vehicle cần {@code make}). Factory Method
 *       tách logic "đọc map → gọi constructor" ra khỏi caller.</li>
 *   <li>Tuân thủ <b>OCP</b>: thêm loại Item mới = thêm class implement interface
 *       này + {@code ItemFactory.register(...)}, không phải sửa code cũ.</li>
 *   <li>Tuân thủ <b>SRP</b>: mỗi creator chỉ biết khởi tạo một loại Item.</li>
 *   <li>Tuân thủ <b>DIP</b>: {@code ItemFactory} chỉ phụ thuộc abstraction này,
 *       không phụ thuộc concrete creator.</li>
 * </ul>
 *
 * <h3>Trade-off</h3>
 * <ul>
 *   <li>Dùng {@code Map<String, String>} giúp creator linh hoạt nhưng mất type-safety
 *       — key sai chính tả không được phát hiện ở compile-time. Bù lại, mỗi creator
 *       chủ động fallback về default value khi key thiếu/format sai để tránh crash.</li>
 *   <li>Số lượng class tăng (mỗi loại Item → 1 creator class) — đánh đổi để có
 *       extensibility.</li>
 * </ul>
 */
public interface ItemCreator {

    /**
     * Tạo một {@link Item} từ thông tin cơ bản và các thuộc tính mở rộng.
     *
     * <p>Concrete creator chịu trách nhiệm:
     * <ul>
     *   <li>Đọc các key cần thiết trong {@code extra} (vd Electronics đọc
     *       {@code "brand"}, {@code "model"}, {@code "condition"}).</li>
     *   <li>Fallback về default hợp lý khi key thiếu hoặc value không parse được
     *       (vd {@code "Unknown"} cho String, năm hiện tại cho int) — để tránh
     *       ném exception ở tầng factory.</li>
     *   <li>Gọi constructor của concrete {@link Item} subclass tương ứng.</li>
     * </ul>
     *
     * @param name     tên sản phẩm (bắt buộc, không null)
     * @param desc     mô tả chi tiết sản phẩm
     * @param price    giá khởi điểm (đơn vị tiền tệ phụ thuộc cấu hình hệ thống)
     * @param sellerId ID người bán — phải tồn tại trong hệ thống user
     * @param extra    map các thuộc tính bổ sung đặc thù cho từng loại Item
     *                 (vd {@code brand}, {@code model} cho Electronics;
     *                 {@code artist}, {@code year}, {@code medium} cho Art;
     *                 {@code make}, {@code vehicleModel}, {@code year},
     *                 {@code mileage} cho Vehicle). Có thể empty — creator sẽ
     *                 dùng default cho mọi thuộc tính.
     * @return instance của concrete {@link Item} subclass đã được khởi tạo
     */
    Item create(String name, String desc, double price, String sellerId, Map<String, String> extra);
}
