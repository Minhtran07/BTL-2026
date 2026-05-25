package com.auction.model.item;


/**
 * =====================================================================
 * LỚP Vehicle - PHƯƠNG TIỆN GIAO THÔNG
 * =====================================================================
 *
 * VAI TRÒ TRONG HỆ THỐNG:
 * - Đại diện cho các phương tiện giao thông (ô tô, xe máy, xe tải...)
 *   được rao bán đấu giá.
 * - Là loại Item thứ ba trong hệ thống (cùng với Electronics và Art).
 *
 * MỐI LIÊN HỆ VỚI CÁC CLASS KHÁC:
 * - Kế thừa Item -> có name, description, startingPrice, sellerId, imageUrl.
 * - Kế thừa gián tiếp Entity (id, createdAt, updatedAt).
 * - Thường được tạo qua Factory Pattern (package pattern.factory).
 *
 * OOP ÁP DỤNG:
 * 1. INHERITANCE: extends Item -> kế thừa các thuộc tính/method chung.
 * 2. POLYMORPHISM:
 *    - Override getCategory() trả về ItemCategory.VEHICLE.
 *    - Override printInfo() để in thông tin riêng của xe.
 * 3. ENCAPSULATION: field private + getter/setter.
 *
 * LƯU Ý ĐẶC BIỆT CHO SINH VIÊN:
 * - Field tên là `vehicleModel` (không phải `model`) để TRÁNH NHẦM LẪN
 *   với từ "model" trong MVC (Model-View-Controller). Đây là quy ước
 *   đặt tên rõ ràng giúp code dễ đọc.
 * - `make` (hãng) và `vehicleModel` (đời xe) là khái niệm chuẩn trong
 *   ngành ô tô: vd Toyota Camry -> make = "Toyota", vehicleModel = "Camry".
 */
public class Vehicle extends Item {

    /**
     * serialVersionUID cho Serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Hãng sản xuất xe (vd: "Toyota", "Honda", "Ford", "VinFast"...).
     * Trong tiếng Anh ngành xe, "make" = "manufacturer" = hãng sản xuất.
     * Giá trị mặc định: null.
     */
    private String make;

    /**
     * Tên/dòng xe (vd: "Camry", "Civic", "Mustang", "VF8"...).
     * Đặt tên `vehicleModel` thay vì `model` để tránh trùng với khái niệm
     * Model trong MVC hoặc trong các package khác.
     * Giá trị mặc định: null.
     */
    private String vehicleModel;

    /**
     * Năm sản xuất xe (vd: 2020, 2023).
     * Giá trị mặc định: 0.
     */
    private int year;

    /**
     * Số kilomet đã đi (odometer reading).
     * Đơn vị: km.
     * Giá trị mặc định: 0 (xe mới chưa lăn bánh).
     *
     * Dùng int -> tối đa khoảng 2,1 tỷ km (đủ dùng vì xe không thể đi quá nhiều).
     */
    private int mileage;

    /**
     * Constructor mặc định.
     * Khởi tạo object Vehicle "rỗng".
     */
    public Vehicle() {
        super();
    }

    /**
     * Constructor đầy đủ.
     *
     * @param name          Tên hiển thị của xe (vd: "Toyota Camry 2020").
     * @param description   Mô tả chi tiết (tình trạng, lịch sử, option...).
     * @param startingPrice Giá khởi điểm đấu giá.
     * @param sellerId      Id người bán.
     * @param make          Hãng xe.
     * @param vehicleModel  Dòng xe.
     * @param year          Năm sản xuất.
     * @param mileage       Số km đã đi.
     */
    public Vehicle(String name, String description, double startingPrice,
                   String sellerId, String make, String vehicleModel, int year, int mileage) {
        // Gọi constructor cha (Item) để gán các field chung.
        super(name, description, startingPrice, sellerId);
        // Gán các field riêng của Vehicle.
        this.make = make;
        this.vehicleModel = vehicleModel;
        this.year = year;
        this.mileage = mileage;
    }

    /**
     * Cài đặt phương thức abstract getCategory() của Item.
     * Trả về ItemCategory.VEHICLE để báo đây là phương tiện.
     *
     * @return ItemCategory.VEHICLE.
     */
    @Override
    public ItemCategory getCategory() {
        return ItemCategory.VEHICLE;
    }

    // ================== Getter / Setter (Encapsulation) ==================

    /**
     * Lấy hãng xe.
     * @return Hãng xe (String).
     */
    public String getMake() {
        return make;
    }

    /**
     * Đặt lại hãng xe và đánh dấu đã cập nhật.
     * @param make Hãng xe mới.
     */
    public void setMake(String make) {
        this.make = make;
        markUpdated();
    }

    /**
     * Lấy dòng xe.
     * @return Dòng xe (String).
     */
    public String getVehicleModel() {
        return vehicleModel;
    }

    /**
     * Đặt lại dòng xe và đánh dấu đã cập nhật.
     * @param vehicleModel Dòng xe mới.
     */
    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
        markUpdated();
    }

    /**
     * Lấy năm sản xuất.
     * @return Năm sản xuất (int).
     */
    public int getYear() {
        return year;
    }

    /**
     * Đặt lại năm sản xuất và đánh dấu đã cập nhật.
     * @param year Năm mới.
     */
    public void setYear(int year) {
        this.year = year;
        markUpdated();
    }

    /**
     * Lấy số km đã đi.
     * @return mileage (int) - đơn vị km.
     */
    public int getMileage() {
        return mileage;
    }

    /**
     * Đặt lại số km đã đi và đánh dấu đã cập nhật.
     *
     * Lưu ý: giá trị mileage không nên giảm so với giá trị cũ
     * (xe không thể đi lùi đồng hồ km), nhưng ở đây không kiểm tra
     * để giữ code đơn giản.
     *
     * @param mileage Số km mới.
     */
    public void setMileage(int mileage) {
        this.mileage = mileage;
        markUpdated();
    }

    /**
     * Override printInfo() để in thông tin riêng của Vehicle.
     * Bao gồm: id, name, make, vehicleModel, year, mileage (km), price.
     *
     * @return Chuỗi mô tả chi tiết phương tiện.
     */
    @Override
    public String printInfo() {
        return String.format("Vehicle[id=%s, name=%s, make=%s, model=%s, year=%d, mileage=%dkm, price=%.2f]",
                getId(), getName(), make, vehicleModel, year, mileage, getStartingPrice());
    }
}
