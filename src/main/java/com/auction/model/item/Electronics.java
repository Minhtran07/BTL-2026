package com.auction.model.item;

/**
 * =====================================================================
 * LỚP Electronics - SẢN PHẨM ĐIỆN TỬ
 * =====================================================================
 *
 * VAI TRÒ TRONG HỆ THỐNG:
 * - Đại diện cho một sản phẩm điện tử (điện thoại, laptop, máy ảnh, TV...)
 *   được rao bán trong phiên đấu giá.
 * - Là MỘT TRONG BA loại Item cụ thể trong hệ thống (cùng với Art và Vehicle).
 *
 * MỐI LIÊN HỆ VỚI CÁC CLASS KHÁC:
 * - Kế thừa Item -> có sẵn name, description, startingPrice, sellerId, imageUrl.
 * - Thông qua Item -> kế thừa gián tiếp Entity (có id, createdAt, updatedAt).
 * - Được tạo ra bởi Factory (xem package pattern.factory) thay vì `new` trực tiếp.
 *
 * CÁC NGUYÊN LÝ OOP ÁP DỤNG:
 * 1. INHERITANCE (Kế thừa): `extends Item` -> tự động có các thuộc tính của Item.
 * 2. POLYMORPHISM (Đa hình):
 *    - Override getCategory() trả về ItemCategory.ELECTRONICS.
 *    - Override printInfo() để in thông tin riêng của Electronics.
 *    -> Khi gọi item.printInfo() trên một biến kiểu Item, Java sẽ
 *       tự động chọn đúng method của Electronics nhờ "dynamic dispatch".
 * 3. ENCAPSULATION (Đóng gói): các field private + getter/setter.
 *
 * LƯU Ý CHO SINH VIÊN:
 * - Khi gọi constructor con, dùng `super(...)` để gọi constructor cha
 *   (truyền các thông tin chung như name, description... lên cho Item xử lý).
 * - Setter có gọi markUpdated() (kế thừa từ Entity) để cập nhật thời điểm sửa.
 */
public class Electronics extends Item {

    /**
     * serialVersionUID - mã phiên bản cho Serialization (giống ở Item).
     * Mỗi class kế thừa Serializable đều nên khai báo riêng.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Hãng sản xuất (vd: "Apple", "Samsung", "Sony"...).
     * Giá trị mặc định: null.
     */
    private String brand;

    /**
     * Tên/mã model cụ thể của sản phẩm (vd: "iPhone 15 Pro", "Galaxy S24").
     * Giá trị mặc định: null.
     */
    private String model;

    /**
     * Tình trạng sản phẩm. Các giá trị thường dùng:
     * - "NEW"         : Mới hoàn toàn, chưa bóc seal.
     * - "LIKE_NEW"    : Như mới (đã sử dụng ít).
     * - "USED"        : Đã qua sử dụng bình thường.
     * - "REFURBISHED" : Hàng tân trang (đã được sửa chữa và làm mới).
     *
     * Ở đây dùng String thay vì enum cho linh hoạt (có thể mở rộng dễ).
     * Giá trị mặc định: null.
     */
    private String condition; // NEW, LIKE_NEW, USED, REFURBISHED

    /**
     * Constructor mặc định (không tham số).
     * Gọi constructor không tham số của Item -> Item gọi tiếp super() của Entity.
     * Dùng khi cần tạo đối tượng "rỗng" rồi gán dần các field qua setter.
     */
    public Electronics() {
        super();
    }

    /**
     * Constructor đầy đủ - dùng khi đã biết hết thông tin sản phẩm.
     *
     * @param name          Tên sản phẩm.
     * @param description   Mô tả chi tiết.
     * @param startingPrice Giá khởi điểm đấu giá.
     * @param sellerId      Id người bán.
     * @param brand         Hãng sản xuất.
     * @param model         Model sản phẩm.
     * @param condition     Tình trạng (NEW/LIKE_NEW/USED/REFURBISHED).
     */
    public Electronics(String name, String description, double startingPrice,
                       String sellerId, String brand, String model, String condition) {
        // Gọi constructor của Item để khởi tạo các field chung.
        // KHÔNG dùng this.name = name vì name là field private của Item,
        // ta phải nhờ constructor cha gán giùm.
        super(name, description, startingPrice, sellerId);
        // Sau khi cha xử lý xong, ta gán các field riêng của Electronics.
        this.brand = brand;
        this.model = model;
        this.condition = condition;
    }

    /**
     * Implement (cài đặt) phương thức abstract getCategory() của Item.
     * Luôn trả về ItemCategory.ELECTRONICS vì đây là class Electronics.
     *
     * @return ItemCategory.ELECTRONICS.
     */
    @Override
    public ItemCategory getCategory() {
        return ItemCategory.ELECTRONICS;
    }

    // ================== Getter / Setter (Encapsulation) ==================

    /**
     * Lấy hãng sản xuất.
     * @return Tên hãng (String).
     */
    public String getBrand() {
        return brand;
    }

    /**
     * Đặt lại hãng sản xuất và đánh dấu sản phẩm đã được cập nhật.
     * @param brand Tên hãng mới.
     */
    public void setBrand(String brand) {
        this.brand = brand;
        markUpdated();
    }

    /**
     * Lấy model.
     * @return Model (String).
     */
    public String getModel() {
        return model;
    }

    /**
     * Đặt lại model và đánh dấu cập nhật.
     * @param model Model mới.
     */
    public void setModel(String model) {
        this.model = model;
        markUpdated();
    }

    /**
     * Lấy tình trạng sản phẩm.
     * @return Tình trạng (String): NEW/LIKE_NEW/USED/REFURBISHED.
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Đặt lại tình trạng sản phẩm và đánh dấu cập nhật.
     * @param condition Tình trạng mới.
     */
    public void setCondition(String condition) {
        this.condition = condition;
        markUpdated();
    }

    /**
     * Override printInfo() của Item để in thông tin RIÊNG của Electronics.
     *
     * Khác biệt với printInfo() ở Item: thêm brand, model, condition.
     * Đây là minh chứng cho POLYMORPHISM - cùng tên method nhưng
     * mỗi class con in nội dung khác nhau.
     *
     * @return Chuỗi mô tả chi tiết sản phẩm điện tử.
     */
    @Override
    public String printInfo() {
        return String.format("Electronics[id=%s, name=%s, brand=%s, model=%s, condition=%s, price=%.2f]",
                getId(), getName(), brand, model, condition, getStartingPrice());
    }
}
