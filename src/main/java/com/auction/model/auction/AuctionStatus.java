package com.auction.model.auction;


/**
 * =====================================================================
 * ENUM AuctionStatus - TRẠNG THÁI CỦA MỘT PHIÊN ĐẤU GIÁ
 * =====================================================================
 *
 * VAI TRÒ TRONG HỆ THỐNG:
 * ----------------------
 * Đây là một enum (kiểu liệt kê) dùng để biểu diễn TẤT CẢ các trạng thái
 * có thể có của một phiên đấu giá (Auction). Bất kỳ phiên đấu giá nào
 * trong hệ thống đều phải ở một trong các trạng thái dưới đây tại mọi
 * thời điểm.
 *
 * Việc dùng enum thay cho String/int có 3 lợi ích lớn:
 *   1) An toàn kiểu (type-safe): compiler bắt lỗi nếu gán giá trị sai.
 *   2) Tự tài liệu hóa: đọc code thấy ngay tập giá trị hợp lệ.
 *   3) Dễ dùng trong switch-case, so sánh bằng == (không cần equals()).
 *
 * MỐI LIÊN HỆ VỚI CÁC CLASS KHÁC:
 * -------------------------------
 *  - Class {@link Auction}: có một field {@code status} kiểu AuctionStatus
 *    để biết phiên đang ở giai đoạn nào (mở, đang chạy, đã kết thúc...).
 *  - Class Auction dùng enum này trong các phương thức {@code placeBid()},
 *    {@code start()}, {@code finish()}, {@code cancel()}... để quyết định
 *    hành động có hợp lệ hay không. Ví dụ: chỉ cho phép đặt giá khi
 *    status == RUNNING; chỉ cho phép chuyển sang FINISHED khi đang RUNNING.
 *  - Tầng UI (View/Controller) cũng đọc status để hiển thị badge màu khác
 *    nhau, ẩn/hiện nút "Đặt giá", "Hủy phiên", "Thanh toán"...
 *
 * SƠ ĐỒ CHUYỂN TRẠNG THÁI (STATE MACHINE):
 * ----------------------------------------
 *
 *         OPEN ──start()──▶ RUNNING ──finish()──▶ FINISHED ──markPaid()──▶ PAID
 *          │                   │                      │
 *          │                   │                      │
 *          └────cancel()───────┴──────cancel()────────┴────cancel()──▶ CANCELED
 *
 *  - OPEN: vừa tạo, chưa tới giờ bắt đầu (hoặc chưa kích hoạt).
 *  - RUNNING: đang nhận bid của người tham gia.
 *  - FINISHED: đã hết giờ / đã kết thúc - chốt người thắng nhưng chưa thu tiền.
 *  - PAID: người thắng đã thanh toán xong.
 *  - CANCELED: phiên bị hủy (do người bán hoặc admin) - không tiếp tục được.
 *
 * KIẾN THỨC OOP ÁP DỤNG:
 * ----------------------
 *  - ENUM (enumeration): một dạng class đặc biệt của Java, có hằng số
 *    được liệt kê sẵn (OPEN, RUNNING, ...).
 *  - ENCAPSULATION (đóng gói): trường {@code displayName} private + final,
 *    chỉ truy cập qua getter.
 *  - IMMUTABLE: mỗi instance enum chỉ tồn tại 1 bản duy nhất trong JVM
 *    và không thể bị thay đổi sau khi khởi tạo.
 *
 * @author  Auction Team
 */
public enum AuctionStatus {
    // =====================================================================
    // Danh sách các hằng số (constants) của enum.
    // Mỗi dòng dưới đây vừa là TÊN trạng thái (OPEN, RUNNING...) vừa kèm
    // theo một tên hiển thị tiếng Việt để show ra giao diện cho người dùng.
    // =====================================================================

    /** Phiên vừa được tạo, đang chờ thời điểm bắt đầu - chưa nhận bid. */
    OPEN("Mở"),

    /** Phiên đang diễn ra - cho phép người dùng đặt giá (bid). */
    RUNNING("Đang diễn ra"),

    /** Phiên đã kết thúc (hết giờ hoặc do hệ thống) - không nhận bid nữa. */
    FINISHED("Kết thúc"),

    /** Người thắng đã thanh toán xong - phiên hoàn tất 100%. */
    PAID("Đã thanh toán"),

    /** Phiên bị hủy - dù chưa diễn ra hoặc đang diễn ra cũng dừng. */
    CANCELED("Đã hủy");

    // =====================================================================
    // Field (thuộc tính) của enum
    // =====================================================================

    /**
     * Tên hiển thị (tiếng Việt) cho người dùng cuối nhìn trên giao diện.
     *
     * - {@code private}: che dấu (đóng gói) - không cho code bên ngoài
     *   truy cập trực tiếp, buộc dùng getter.
     * - {@code final}: chỉ gán một lần ở constructor, không thể thay đổi
     *   sau đó. Đảm bảo tính bất biến (immutability) của enum.
     */
    private final String displayName;

    // =====================================================================
    // Constructor
    // =====================================================================

    /**
     * Constructor của enum.
     *
     * <p>LƯU Ý: constructor của enum LUÔN là {@code private} ngầm định.
     * Java sẽ tự gọi constructor này một lần cho mỗi hằng số bên trên
     * (OPEN, RUNNING, FINISHED, PAID, CANCELED) khi class được load.
     * Người dùng không thể tạo thêm instance mới của enum.
     *
     * @param displayName chuỗi tiếng Việt mô tả trạng thái (ví dụ "Mở")
     */
    AuctionStatus(String displayName) {
        // Gán tên hiển thị tiếng Việt cho hằng số tương ứng.
        this.displayName = displayName;
    }

    // =====================================================================
    // Getter
    // =====================================================================

    /**
     * Lấy tên hiển thị tiếng Việt của trạng thái.
     *
     * <p>Ví dụ: {@code AuctionStatus.RUNNING.getDisplayName()} trả về
     * chuỗi {@code "Đang diễn ra"} - dùng để hiển thị trên label, badge
     * hoặc cột bảng trong giao diện JavaFX.
     *
     * @return chuỗi tiếng Việt mô tả trạng thái
     */
    public String getDisplayName() {
        return displayName;
    }
}
