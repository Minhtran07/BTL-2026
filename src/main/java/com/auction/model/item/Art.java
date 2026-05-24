package com.auction.model.item;


/**
 * =====================================================================
 * LỚP Art - SẢN PHẨM NGHỆ THUẬT
 * =====================================================================
 *
 * VAI TRÒ TRONG HỆ THỐNG:
 * - Đại diện cho các sản phẩm nghệ thuật (tranh, tượng, đồ thủ công,
 *   tác phẩm điêu khắc...) được rao bán đấu giá.
 * - Là một trong ba loại Item cụ thể (cùng với Electronics và Vehicle).
 *
 * MỐI LIÊN HỆ VỚI CÁC CLASS KHÁC:
 * - Kế thừa Item -> có sẵn name, description, startingPrice, sellerId, imageUrl.
 * - Kế thừa gián tiếp Entity (id, createdAt, updatedAt).
 * - Thường được tạo qua Factory Pattern (package pattern.factory).
 *
 * OOP ÁP DỤNG:
 * 1. INHERITANCE: extends Item -> tái sử dụng code đã có.
 * 2. POLYMORPHISM:
 *    - Override getCategory() trả về ItemCategory.ART.
 *    - Override printInfo() để in thông tin riêng của Art.
 * 3. ENCAPSULATION: field private + getter/setter có markUpdated().
 *
 * LƯU Ý CHO SINH VIÊN:
 * - Đây là một ví dụ điển hình của "is-a relationship":
 *   "Một Art LÀ MỘT Item" -> dùng inheritance.
 * - Nếu sau này thêm loại sản phẩm mới (vd: BookItem), chỉ cần
 *   tạo class mới extends Item, không cần sửa class cũ -> OPEN/CLOSED PRINCIPLE.
 */
public class Art extends Item {

    /**
     * serialVersionUID cho Serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Tên tác giả/họa sĩ (vd: "Bùi Xuân Phái", "Picasso"...).
     * Giá trị mặc định: null.
     */
    private String artist;

    /**
     * Năm sáng tác (vd: 1990, 2020).
     * Kiểu int -> không cho giá trị thập phân (năm phải là số nguyên).
     * Giá trị mặc định: 0 (vì int luôn mặc định 0).
     *
     * Lưu ý: 0 có thể gây nhầm lẫn (không có năm 0 trong lịch),
     * nên ngoài UI cần xử lý hiển thị khi year == 0 (vd: "Chưa rõ năm").
     */
    private int year;

    /**
     * Chất liệu/kỹ thuật dùng để tạo ra tác phẩm. Một số giá trị thường gặp:
     * - "Oil"        : Sơn dầu.
     * - "Watercolor" : Màu nước.
     * - "Digital"    : Kỹ thuật số.
     * - "Acrylic"    : Sơn acrylic.
     * - "Sculpture"  : Điêu khắc...
     *
     * Giá trị mặc định: null.
     */
    private String medium; // Oil, Watercolor, Digital, etc.

    /**
     * Constructor mặc định.
     * Dùng khi muốn tạo object Art "rỗng" rồi set dần các field.
     */
    public Art() {
        super();
    }

    /**
     * Constructor đầy đủ.
     *
     * @param name          Tên tác phẩm.
     * @param description   Mô tả chi tiết.
     * @param startingPrice Giá khởi điểm.
     * @param sellerId      Id người bán.
     * @param artist        Tên tác giả.
     * @param year          Năm sáng tác.
     * @param medium        Chất liệu/kỹ thuật.
     */
    public Art(String name, String description, double startingPrice,
               String sellerId, String artist, int year, String medium) {
        // Gọi constructor cha (Item) để gán các thuộc tính chung.
        super(name, description, startingPrice, sellerId);
        // Gán các field riêng của Art.
        this.artist = artist;
        this.year = year;
        this.medium = medium;
    }

    /**
     * Cài đặt phương thức abstract getCategory() của Item.
     * Trả về ItemCategory.ART để báo đây là sản phẩm nghệ thuật.
     *
     * @return ItemCategory.ART.
     */
    @Override
    public ItemCategory getCategory() {
        return ItemCategory.ART;
    }

    // ================== Getter / Setter (Encapsulation) ==================

    /**
     * Lấy tên tác giả.
     * @return Tên tác giả (String).
     */
    public String getArtist() {
        return artist;
    }

    /**
     * Đặt lại tên tác giả và đánh dấu cập nhật.
     * @param artist Tên tác giả mới.
     */
    public void setArtist(String artist) {
        this.artist = artist;
        markUpdated();
    }

    /**
     * Lấy năm sáng tác.
     * @return Năm dạng int.
     */
    public int getYear() {
        return year;
    }

    /**
     * Đặt lại năm sáng tác và đánh dấu cập nhật.
     *
     * Lưu ý: nên thêm kiểm tra year > 0 và year <= năm hiện tại,
     * nhưng ở đây giữ đơn giản.
     *
     * @param year Năm mới.
     */
    public void setYear(int year) {
        this.year = year;
        markUpdated();
    }

    /**
     * Lấy chất liệu/kỹ thuật.
     * @return Chất liệu (String).
     */
    public String getMedium() {
        return medium;
    }

    /**
     * Đặt lại chất liệu/kỹ thuật và đánh dấu cập nhật.
     * @param medium Chất liệu mới.
     */
    public void setMedium(String medium) {
        this.medium = medium;
        markUpdated();
    }

    /**
     * Override printInfo() để in thông tin riêng của Art.
     * Bao gồm: id, name, artist, year, medium, price.
     *
     * @return Chuỗi mô tả chi tiết tác phẩm nghệ thuật.
     */
    @Override
    public String printInfo() {
        return String.format("Art[id=%s, name=%s, artist=%s, year=%d, medium=%s, price=%.2f]",
                getId(), getName(), artist, year, medium, getStartingPrice());
    }
}
