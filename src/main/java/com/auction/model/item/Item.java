package com.auction.model.item;

// Import lớp Entity - lớp cha trừu tượng chứa các thuộc tính chung như:
// id (định danh duy nhất), createdAt (thời điểm tạo), updatedAt (thời điểm cập nhật).
// Mọi đối tượng nghiệp vụ trong hệ thống đấu giá đều kế thừa Entity để có
// các thuộc tính "căn cước" này, tránh phải viết đi viết lại trong từng class con.
import com.auction.model.entity.Entity;

/**
 * =====================================================================
 * LỚP TRỪU TƯỢNG (ABSTRACT CLASS) Item - SẢN PHẨM ĐẤU GIÁ
 * =====================================================================
 *
 * VAI TRÒ TRONG HỆ THỐNG:
 * - Item đại diện cho MỘT SẢN PHẨM được rao bán đấu giá trong hệ thống.
 * - Đây là "khuôn mẫu chung" cho tất cả các loại sản phẩm: Điện tử
 *   (Electronics), Nghệ thuật (Art), Phương tiện (Vehicle).
 * - Item KHÔNG thể được khởi tạo trực tiếp (vì là abstract). Muốn tạo
 *   một sản phẩm cụ thể, ta phải tạo một subclass (Electronics, Art,
 *   Vehicle...) hoặc dùng Factory Pattern (xem package pattern.factory).
 *
 * MỐI LIÊN HỆ VỚI CÁC CLASS KHÁC:
 * - Kế thừa từ Entity -> được thừa hưởng id, createdAt, updatedAt,
 *   các phương thức getId(), markUpdated()...
 * - Là cha của Electronics, Art, Vehicle -> đa hình (polymorphism).
 * - Liên kết với User thông qua sellerId (id của người bán).
 * - Liên kết với enum ItemCategory để phân loại sản phẩm.
 * - Được sử dụng trong Auction (phiên đấu giá), Bid (lượt đặt giá)...
 *
 * CÁC NGUYÊN LÝ OOP ÁP DỤNG TRONG CLASS NÀY:
 * 1. INHERITANCE (Kế thừa): Item kế thừa Entity; các subclass kế thừa Item.
 * 2. POLYMORPHISM (Đa hình): Phương thức getCategory() là abstract,
 *    mỗi subclass sẽ trả về một danh mục khác nhau. Phương thức printInfo()
 *    cũng được override ở mỗi subclass để in thông tin riêng.
 * 3. ENCAPSULATION (Đóng gói): Tất cả các field đều để private và truy cập
 *    thông qua getter/setter, đảm bảo dữ liệu được kiểm soát.
 * 4. ABSTRACTION (Trừu tượng): Class này là abstract, định nghĩa khung
 *    chung mà các subclass phải tuân theo.
 *
 * LƯU Ý CHO SINH VIÊN:
 * - "abstract class" nghĩa là KHÔNG thể `new Item(...)` được. Bắt buộc
 *   phải `new Electronics(...)` hoặc subclass khác.
 * - Lý do thiết kế abstract: vì khái niệm "Item" quá chung chung,
 *   không có ý nghĩa nếu không biết là loại sản phẩm gì.
 */
public abstract class Item extends Entity {

    /**
     * serialVersionUID - "Mã phiên bản" dùng cho cơ chế Serialization
     * (chuyển đối tượng thành luồng byte để lưu file hoặc gửi qua mạng).
     *
     * Giá trị mặc định: 1L (chữ L để báo cho Java đây là kiểu long).
     *
     * Vì sao cần? Khi đọc lại đối tượng từ file, Java sẽ so sánh
     * serialVersionUID của file với của class hiện tại. Nếu khớp -> OK,
     * không khớp -> báo lỗi InvalidClassException.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Tên sản phẩm (vd: "iPhone 15 Pro Max", "Tranh Đông Hồ"...).
     * Giá trị mặc định: null (khi dùng constructor không tham số).
     */
    private String name;

    /**
     * Mô tả chi tiết về sản phẩm (vd: "Máy còn 99%, full box...").
     * Giá trị mặc định: null.
     */
    private String description;

    /**
     * Giá khởi điểm của phiên đấu giá (đơn vị tiền, vd: VND hoặc USD).
     * Kiểu double để hỗ trợ số thập phân (vd: 1500000.50).
     * Giá trị mặc định: 0.0 (vì double luôn mặc định 0.0).
     */
    private double startingPrice;

    /**
     * ID của người bán (chính là id của User đang rao bán).
     * Dùng để biết SẢN PHẨM NÀY THUỘC VỀ AI.
     * Giá trị mặc định: null.
     */
    private String sellerId;

    /**
     * Đường dẫn (URL) tới ảnh đại diện của sản phẩm.
     * Có thể là link internet hoặc đường dẫn file local.
     * Giá trị mặc định: null (sản phẩm có thể không có ảnh).
     */
    private String imageUrl;

    /**
     * Constructor mặc định (không tham số).
     *
     * Vì sao để `protected` thay vì `public`?
     * - `protected` nghĩa là chỉ các subclass (Electronics, Art, Vehicle)
     *   và class trong cùng package mới gọi được.
     * - Tránh việc code bên ngoài tự ý tạo Item "rỗng".
     *
     * `super()` -> gọi constructor của lớp cha Entity để khởi tạo
     * các thuộc tính chung (id, createdAt, updatedAt).
     */
    protected Item() {
        super();
    }

    /**
     * Constructor đầy đủ tham số chính - dùng khi tạo Item mới
     * với thông tin cơ bản (chưa cần imageUrl).
     *
     * @param name          Tên sản phẩm.
     * @param description   Mô tả sản phẩm.
     * @param startingPrice Giá khởi điểm.
     * @param sellerId      Id của người bán.
     */
    protected Item(String name, String description, double startingPrice, String sellerId) {
        // Gọi constructor cha trước để khởi tạo id, createdAt, updatedAt.
        super();
        // Sau đó gán các giá trị riêng của Item.
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerId = sellerId;
    }

    /**
     * Phương thức TRỪU TƯỢNG (abstract) trả về danh mục sản phẩm.
     *
     * Vì sao abstract?
     * - Item không thể tự biết mình thuộc danh mục nào (vì nó là khái niệm chung).
     * - Bắt buộc mỗi subclass PHẢI implement phương thức này, trả về
     *   danh mục tương ứng (Electronics -> ELECTRONICS, Art -> ART...).
     *
     * Đây chính là POLYMORPHISM (đa hình): cùng một method nhưng
     * mỗi class con trả về giá trị khác nhau.
     *
     * @return Danh mục sản phẩm (enum ItemCategory).
     */
    public abstract ItemCategory getCategory();

    // ================== Encapsulation - Getter / Setter ==================
    // Vì sao cần getter/setter thay vì để field public?
    // - Bảo vệ dữ liệu: có thể thêm logic kiểm tra trong setter.
    // - Ở đây setter còn gọi markUpdated() để cập nhật thời gian sửa đổi.

    /**
     * Lấy tên sản phẩm.
     * @return Tên sản phẩm dạng String.
     */
    public String getName() {
        return name;
    }

    /**
     * Đặt lại tên sản phẩm.
     * Sau khi đổi -> gọi markUpdated() để ghi nhận thời điểm sửa đổi
     * (nhằm theo dõi lịch sử thay đổi của entity).
     *
     * @param name Tên mới.
     */
    public void setName(String name) {
        this.name = name;
        markUpdated();
    }

    /**
     * Lấy mô tả sản phẩm.
     * @return Mô tả dạng String.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Đặt lại mô tả sản phẩm và đánh dấu đã cập nhật.
     * @param description Mô tả mới.
     */
    public void setDescription(String description) {
        this.description = description;
        markUpdated();
    }

    /**
     * Lấy giá khởi điểm.
     * @return Giá khởi điểm dạng double.
     */
    public double getStartingPrice() {
        return startingPrice;
    }

    /**
     * Đặt lại giá khởi điểm.
     * Lưu ý: trong thực tế nên kiểm tra giá > 0, nhưng ở đây
     * chưa thêm kiểm tra để giữ code đơn giản.
     *
     * @param startingPrice Giá khởi điểm mới.
     */
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
        markUpdated();
    }

    /**
     * Lấy id của người bán.
     * @return sellerId (String).
     */
    public String getSellerId() {
        return sellerId;
    }

    /**
     * Đặt lại người bán.
     * Lưu ý: KHÔNG gọi markUpdated() ở đây - có thể tác giả code
     * coi sellerId là thông tin ít khi thay đổi (gần như cố định).
     *
     * @param sellerId Id của người bán mới.
     */
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    /**
     * Lấy đường dẫn ảnh sản phẩm.
     * @return URL ảnh dạng String.
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Đặt lại URL ảnh sản phẩm.
     * @param imageUrl Đường dẫn ảnh mới.
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        markUpdated();
    }

    /**
     * Override (ghi đè) phương thức printInfo() từ lớp cha Entity.
     *
     * Mục đích: trả về một chuỗi mô tả thông tin tổng quan của Item
     * (id, tên, danh mục, giá khởi điểm).
     *
     * Vì sao override? Vì Entity có phương thức printInfo() chung chung,
     * còn ở Item ta muốn in thêm các thông tin riêng (name, category...).
     *
     * Lưu ý: gọi getCategory() ở đây sẽ tự động gọi method ở subclass
     * (Electronics, Art, Vehicle) nhờ POLYMORPHISM.
     *
     * @return Chuỗi mô tả thông tin sản phẩm.
     */
    @Override
    public String printInfo() {
        return String.format("Item[id=%s, name=%s, category=%s, startPrice=%.2f]",
                getId(), name, getCategory(), startingPrice);
    }
}
