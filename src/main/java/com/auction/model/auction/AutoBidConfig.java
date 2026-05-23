package com.auction.model.auction;



import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * =====================================================================
 * CLASS AutoBidConfig - CẤU HÌNH "TỰ ĐỘNG ĐẶT GIÁ" (PROXY BIDDING)
 * =====================================================================
 *
 * VAI TRÒ TRONG HỆ THỐNG:
 * ----------------------
 * Class này lưu thông số cho tính năng "auto-bid" (đặt giá tự động).
 * Người dùng đăng ký một mức giá TRẦN (maxBid) và bước nhảy giá
 * (increment). Khi có ai đó đặt giá cao hơn người dùng, hệ thống sẽ
 * TỰ ĐỘNG đặt giá thay cho người dùng theo bước increment, cho đến khi
 * vượt qua mức trần maxBid thì dừng lại.
 *
 * Đây là mô hình "proxy bidding" - giống eBay: người dùng không cần
 * canh giờ ngồi đấu giá, mà hệ thống "đại diện" họ đặt giá tự động.
 *
 * VÍ DỤ THỰC TẾ:
 * -------------
 *  - Phiên hiện tại đang là 100.000đ.
 *  - Bidder A đăng ký auto-bid với maxBid = 200.000đ, increment = 10.000đ.
 *  - Bidder B đặt giá 110.000đ.
 *  - Hệ thống tự động đặt 120.000đ thay cho A (vì 120k <= 200k).
 *  - Bidder B đặt 130.000đ → hệ thống tự đặt 140.000đ cho A.
 *  - Cứ thế cho đến khi mức giá vượt qua 200.000đ thì A "bỏ cuộc".
 *
 * MỐI LIÊN HỆ VỚI CÁC CLASS KHÁC:
 * -------------------------------
 *  - {@link Auction}: chứa một danh sách {@code List<AutoBidConfig> autoBids}.
 *    Mỗi phiên đấu giá có 0..n cấu hình auto-bid của nhiều bidder.
 *  - {@link Auction#processAutoBids(String)}: phương thức duyệt qua danh
 *    sách autoBids để sinh ra bid tự động sau mỗi lần có bid mới.
 *  - {@link com.auction.model.transaction.BidTransaction}: mỗi lần auto-bid
 *    được kích hoạt sẽ tạo ra một BidTransaction mới.
 *
 * KIẾN THỨC OOP ÁP DỤNG:
 * ----------------------
 *  - ENCAPSULATION: tất cả field đều {@code private final}, chỉ truy cập
 *    qua getter - không có setter (bất biến hoàn toàn).
 *  - IMMUTABLE OBJECT: object sau khi tạo là không thể thay đổi. Đây là
 *    pattern rất tốt để dùng trong môi trường đa luồng (thread-safe
 *    miễn phí - không cần lock).
 *  - SERIALIZABLE: cài đặt {@link Serializable} để có thể lưu xuống file
 *    hoặc gửi qua mạng (RMI / socket trong hệ thống đấu giá phân tán).
 *
 * TẠI SAO IMMUTABLE?
 * ------------------
 * Nếu cho phép sửa maxBid sau khi đăng ký, có thể xảy ra race condition:
 * thread A đọc maxBid = 200k, đang tính toán thì thread B sửa thành 150k,
 * dẫn đến hệ thống vẫn đặt bid 180k vi phạm giới hạn mới. Bằng cách làm
 * immutable, ta loại bỏ hoàn toàn rủi ro này: muốn đổi cấu hình thì
 * phải tạo AutoBidConfig MỚI và thay thế.
 *
 * @author  Auction Team
 */
public class AutoBidConfig implements Serializable {

    /**
     * UID cho cơ chế serialization của Java.
     *
     * <p>Khi class này được ghi ra file/network rồi đọc lại, JVM dùng
     * {@code serialVersionUID} để xác minh phiên bản class có khớp không.
     * Nếu không khớp → ném InvalidClassException. Tự đặt giá trị giúp
     * tránh việc thay đổi nhỏ trong class làm vỡ tương thích.
     */
    private static final long serialVersionUID = 1L;

    // =====================================================================
    // CÁC THUỘC TÍNH (FIELDS) - tất cả đều private final (immutable)
    // =====================================================================

    /**
     * ID duy nhất của bidder (người tham gia đấu giá) đăng ký auto-bid.
     *
     * <p>Dùng để định danh: khi xử lý auto-bid, hệ thống cần biết bid
     * tự động này thuộc về user nào để cập nhật {@code currentHighestBidderId}
     * trong {@link Auction}.
     *
     * <p>Ràng buộc: phải tham chiếu đến một User đang tồn tại và phải
     * khác với sellerId của phiên (người bán không được tự đấu hàng).
     */
    private final String bidderId;

    /**
     * Tên hiển thị (display name) của bidder.
     *
     * <p>Lưu thẳng ở đây (chứ không chỉ lưu ID rồi join) để giảm số lần
     * tra cứu khi hiển thị lịch sử bid - tối ưu cho UI realtime.
     */
    private final String bidderName;

    /**
     * Mức giá TRẦN tối đa mà bidder sẵn sàng trả.
     *
     * <p>Khi hệ thống đặt auto-bid, nếu mức giá mới (currentHighestBid +
     * increment) VƯỢT QUÁ maxBid thì DỪNG - không đặt nữa, coi như bidder
     * "bỏ cuộc". Đây là cơ chế bảo vệ người dùng không bị "cuốn theo
     * cảm xúc" trả giá quá tay.
     *
     * <p>Ràng buộc: maxBid phải > giá hiện tại tại thời điểm đăng ký.
     */
    private final double maxBid;

    /**
     * Bước giá - mỗi lần auto-bid sẽ tăng thêm bao nhiêu.
     *
     * <p>Ví dụ increment = 10.000đ thì mỗi lần auto-bid sinh ra một bid
     * mới = currentHighestBid + 10.000đ.
     *
     * <p>LƯU Ý: trong {@link Auction#processAutoBids(String)}, hệ thống
     * sẽ ép buộc bước giá tối thiểu = 1% giá hiện tại để tránh spam tăng
     * giá quá nhỏ (vi tăng). Nếu increment ở đây < 1% giá hiện tại thì
     * 1% sẽ được dùng làm bước hiệu lực.
     *
     * <p>Ràng buộc: increment > 0.
     */
    private final double increment;

    /**
     * Thời điểm bidder đăng ký auto-bid này.
     *
     * <p>Dùng làm "tiebreaker" (yếu tố phân định) trong
     * {@link Auction#processAutoBids(String)}: nếu hai auto-bidder cùng
     * đủ điều kiện đặt giá ở vòng lặp hiện tại, ai đăng ký TRƯỚC sẽ được
     * đặt giá trước (FIFO - first in, first out) - đảm bảo công bằng.
     */
    private final LocalDateTime registeredAt;

    // =====================================================================
    // CONSTRUCTOR
    // =====================================================================

    /**
     * Khởi tạo cấu hình auto-bid mới.
     *
     * <p>Constructor này KHÔNG nhận thời gian đăng ký từ bên ngoài - mà tự
     * dùng {@link LocalDateTime#now()} để đảm bảo không ai "gian lận" đăng
     * ký với thời gian giả (ví dụ khai báo registeredAt sớm hơn để
     * giành lượt ưu tiên trong tiebreaker).
     *
     * @param bidderId    ID duy nhất của người đăng ký auto-bid
     * @param bidderName  Tên hiển thị của người đăng ký
     * @param maxBid      Mức giá trần - hệ thống không vượt quá giá này
     * @param increment   Bước giá mỗi lần auto-bid (tối thiểu 1% giá hiện tại)
     */
    public AutoBidConfig(String bidderId, String bidderName, double maxBid, double increment) {
        // Gán các tham số vào field (tất cả đều final - chỉ gán được 1 lần)
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.maxBid = maxBid;
        this.increment = increment;
        // Ghi nhận thời điểm đăng ký = thời điểm hiện tại của hệ thống.
        // Dùng để xếp thứ tự ưu tiên (FIFO) khi nhiều auto-bidder cạnh tranh.
        this.registeredAt = LocalDateTime.now();
    }

    // =====================================================================
    // GETTERS - không có setter vì class này IMMUTABLE
    // =====================================================================

    /**
     * @return ID của bidder đăng ký auto-bid
     */
    public String getBidderId() {
        return bidderId;
    }

    /**
     * @return Tên hiển thị của bidder
     */
    public String getBidderName() {
        return bidderName;
    }

    /**
     * @return Mức giá trần - hệ thống dừng auto-bid khi vượt giá này
     */
    public double getMaxBid() {
        return maxBid;
    }

    /**
     * @return Bước giá tăng mỗi lần auto-bid
     */
    public double getIncrement() {
        return increment;
    }

    /**
     * @return Thời điểm đăng ký cấu hình auto-bid - dùng làm tiebreaker FIFO
     */
    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    // =====================================================================
    // toString() - in thông tin gọn gàng cho mục đích debug / log
    // =====================================================================

    /**
     * Tạo chuỗi mô tả ngắn gọn cấu hình - dùng cho log/debug.
     *
     * <p>OVERRIDE phương thức {@code toString()} mặc định của {@link Object}
     * - đây là một ví dụ điển hình của tính ĐA HÌNH (polymorphism) trong OOP.
     *
     * @return chuỗi dạng "AutoBid[bidder=..., maxBid=..., increment=...]"
     */
    @Override
    public String toString() {
        return String.format("AutoBid[bidder=%s, maxBid=%.2f, increment=%.2f]",
                bidderName, maxBid, increment);
    }
}
