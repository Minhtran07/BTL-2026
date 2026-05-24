package com.auction.model.item;

/**
 * =====================================================================
 * ENUM ItemCategory - DANH MỤC SẢN PHẨM ĐẤU GIÁ
 * =====================================================================
 *
 * VAI TRÒ TRONG HỆ THỐNG:
 * - Đây là một ENUM (kiểu liệt kê) - dùng để định nghĩa MỘT TẬP HỢP
 *   CỐ ĐỊNH các danh mục sản phẩm. Người dùng KHÔNG thể tự thêm danh
 *   mục mới ở runtime mà phải sửa source code và compile lại.
 * - Mỗi sản phẩm (Item) thuộc về duy nhất một danh mục thông qua
 *   phương thức getCategory() ở các subclass (Electronics, Art, Vehicle).
 *
 * VÌ SAO DÙNG ENUM MÀ KHÔNG DÙNG STRING/INT?
 * 1. AN TOÀN KIỂU (type-safe): nếu dùng String "ELECTRONICS" thì có
 *    thể bị viết sai thành "electronic" mà compiler không phát hiện.
 *    Dùng enum -> compiler kiểm tra được.
 * 2. DỄ BẢO TRÌ: muốn thêm danh mục mới chỉ cần thêm vào enum.
 * 3. CÓ THỂ GẮN DỮ LIỆU/HÀNH VI: mỗi giá trị enum có thể có field
 *    và method riêng (như displayName ở dưới).
 *
 * MỐI LIÊN HỆ VỚI CÁC CLASS KHÁC:
 * - Được dùng trong abstract method Item.getCategory().
 * - Được Electronics, Art, Vehicle trả về để báo loại của mình.
 * - Có thể dùng ở UI để hiển thị tên tiếng Việt cho người dùng
 *   (qua getDisplayName()).
 *
 * OOP ÁP DỤNG:
 * - ENCAPSULATION: field displayName là private final, chỉ truy cập
 *   qua getter -> không sửa được sau khi tạo (immutable).
 */
public enum ItemCategory {

    /**
     * Danh mục Điện tử - tương ứng class Electronics.
     * Hiển thị tiếng Việt: "Điện tử".
     */
    ELECTRONICS("Điện tử"),

    /**
     * Danh mục Nghệ thuật - tương ứng class Art.
     * Bao gồm tranh, tượng, đồ sưu tầm nghệ thuật...
     */
    ART("Nghệ thuật"),

    /**
     * Danh mục Phương tiện - tương ứng class Vehicle.
     * Bao gồm ô tô, xe máy, xe đạp...
     */
    VEHICLE("Phương tiện"),

    /**
     * Danh mục Khác - dùng cho các sản phẩm chưa được phân loại
     * cụ thể. Là "phương án dự phòng" để tránh thiếu danh mục.
     */
    OTHER("Khác");

    /**
     * Tên hiển thị tiếng Việt của danh mục (để show ra giao diện).
     *
     * `final` -> không thay đổi được sau khi khởi tạo.
     * `private` -> đóng gói, chỉ truy cập qua getter.
     *
     * Giá trị mặc định: được truyền vào ngay khi định nghĩa enum
     * (ELECTRONICS("Điện tử"), ART("Nghệ thuật")...).
     */
    private final String displayName;

    /**
     * Constructor của enum (luôn là private/package-private, không thể public).
     *
     * Vì sao? Vì enum chỉ được Java khởi tạo MỘT LẦN cho mỗi giá trị,
     * không cho phép code bên ngoài tự `new ItemCategory(...)`.
     *
     * @param displayName Tên hiển thị tiếng Việt.
     */
    ItemCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Trả về tên hiển thị tiếng Việt của danh mục.
     *
     * Ví dụ:
     *   ItemCategory.ELECTRONICS.getDisplayName() -> "Điện tử"
     *
     * @return Tên hiển thị (String).
     */
    public String getDisplayName() {
        return displayName;
    }
}
