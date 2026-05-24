package com.auction.model.transaction;



import com.auction.model.entity.Entity;

import java.time.LocalDateTime;

/**
 * =====================================================================
 * CLASS BidTransaction - GIAO DỊCH ĐẶT GIÁ (MỘT LẦN BID)
 * =====================================================================
 *
 * VAI TRÒ TRONG HỆ THỐNG:
 * ----------------------
 * Mỗi khi một người dùng "đặt giá" trong một phiên đấu giá (cho dù là
 * đặt thủ công bằng tay hay do hệ thống tự động đặt thông qua auto-bid),
 * hệ thống sẽ tạo ra MỘT đối tượng {@code BidTransaction} để LƯU LẠI
 * vĩnh viễn lần đặt giá đó. Object này giống như một "phiếu giao dịch"
 * - ghi lại đầy đủ ai bid, bid bao nhiêu, vào lúc nào, và giá trước đó
 * là bao nhiêu.
 *
 * Tập hợp tất cả BidTransaction của một Auction chính là LỊCH SỬ ĐẤU GIÁ
 * - dùng để hiển thị biểu đồ tăng giá, danh sách bid, xác định người
 * thắng cuộc, đối chiếu khi tranh chấp, audit, ...
 *
 * MỐI LIÊN HỆ VỚI CÁC CLASS KHÁC:
 * -------------------------------
 *  - {@link Entity} (cha): cung cấp ID duy nhất, createdAt, updatedAt.
 *    BidTransaction kế thừa nên cũng có những thuộc tính chung này.
 *  - {@link com.auction.model.auction.Auction}: chứa
 *    {@code List<BidTransaction> bidHistory}. Mỗi BidTransaction biết
 *    nó thuộc về Auction nào qua {@code auctionId}.
 *  - {@link com.auction.model.auction.AutoBidConfig}: khi auto-bid kích
 *    hoạt cũng sinh ra BidTransaction (dùng constructor có bidTime tường minh).
 *
 * KIẾN THỨC OOP ÁP DỤNG:
 * ----------------------
 *  - INHERITANCE (kế thừa): extends {@link Entity} → tái sử dụng code
 *    quản lý ID, timestamps, serialization.
 *  - ENCAPSULATION: tất cả field private final, chỉ getter, không có
 *    setter → bất biến (immutable).
 *  - IMMUTABLE OBJECT: một bid đã đặt thì KHÔNG được phép sửa nội dung
 *    (mô phỏng đúng quy tắc đấu giá ngoài đời - không có chuyện hồi tố).
 *    Mọi field đều {@code final}.
 *  - CONSTRUCTOR OVERLOADING (nạp chồng): có 2 constructor - một dùng
 *    thời gian hiện tại tự động, một cho phép truyền thời gian tường minh.
 *  - POLYMORPHISM: override {@code printInfo()} từ class cha Entity.
 *
 * @author  Auction Team
 */
public class BidTransaction extends Entity {

    /**
     * UID cho serialization - đảm bảo tương thích nhị phân giữa các phiên
     * bản class khi đọc/ghi file (object I/O) hoặc truyền qua mạng.
     */
    private static final long serialVersionUID = 1L;

    // =====================================================================
    // CÁC TRƯỜNG DỮ LIỆU (FIELDS)
    //
    // Tất cả đều {@code private final} → IMMUTABLE: sau khi tạo không
    // ai có thể đổi giá trị nữa, đảm bảo tính toàn vẹn của bản ghi đấu giá.
    // =====================================================================

    /**
     * ID của phiên đấu giá (Auction) mà bid này thuộc về.
     *
     * <p>Đây là "khóa ngoại" trỏ đến Auction. Khi cần lấy tất cả bid của
     * một phiên, ta lọc danh sách BidTransaction theo trường này.
     */
    private final String auctionId;

    /**
     * ID của người đặt giá (bidder).
     *
     * <p>Khóa ngoại trỏ đến User. Phải khác sellerId của Auction (người bán
     * không được tự đấu hàng của chính mình - đã được kiểm tra trong
     * {@link com.auction.model.auction.Auction#placeBid(String, String, double)}).
     */
    private final String bidderId;

    /**
     * Tên hiển thị của người đặt giá tại thời điểm bid.
     *
     * <p>Lưu trực tiếp ở đây (chứ không chỉ lưu ID rồi join sau) để:
     *   1) Tăng tốc render UI (không phải tra cứu thêm).
     *   2) Bảo toàn lịch sử: nếu sau này user đổi tên, lịch sử bid vẫn
     *      hiển thị đúng tên dùng tại thời điểm đặt giá đó.
     */
    private final String bidderName;

    /**
     * Số tiền của lần đặt giá này (số tiền MỚI, sau khi tăng).
     *
     * <p>Ràng buộc nghiệp vụ: bidAmount > previousBid (đã được kiểm tra
     * trong Auction.placeBid trước khi tạo BidTransaction).
     */
    private final double bidAmount;

    /**
     * Giá cao nhất TRƯỚC lần bid này (snapshot).
     *
     * <p>Lưu lại để có thể tính được "delta" (mức tăng) mà không cần phải
     * tra cứu bid trước trong lịch sử. Rất tiện cho biểu đồ và báo cáo.
     */
    private final double previousBid;

    /**
     * Thời điểm chính xác của lần đặt giá này.
     *
     * <p>Sử dụng {@link LocalDateTime} có độ phân giải nano-giây để đảm
     * bảo có thể phân biệt được nhiều bid xảy ra trong cùng một giây
     * wall-clock (khi auto-bid sinh ra liên tục trong một burst).
     */
    private final LocalDateTime bidTime;

    // =====================================================================
    // CÁC CONSTRUCTOR (NẠP CHỒNG - OVERLOADING)
    // =====================================================================

    /**
     * Constructor cơ bản - dùng cho bid THỦ CÔNG do người dùng đặt.
     *
     * <p>Thời điểm bid được lấy tự động từ đồng hồ hệ thống tại thời
     * điểm tạo object ({@code LocalDateTime.now()}). Đây là cách dùng
     * phổ biến nhất trong code chính.
     *
     * @param auctionId   ID của phiên đấu giá
     * @param bidderId    ID của người đặt giá
     * @param bidderName  Tên hiển thị người đặt giá
     * @param bidAmount   Số tiền của lần bid này
     * @param previousBid Giá cao nhất trước lần bid này
     */
    public BidTransaction(String auctionId, String bidderId, String bidderName,
                          double bidAmount, double previousBid) {
        // Gọi constructor của lớp cha Entity để tự sinh ID + timestamps chung.
        super();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.previousBid = previousBid;
        // Lấy thời gian hệ thống ngay lúc tạo object → "thời điểm bid".
        this.bidTime = LocalDateTime.now();
    }

    /**
     * Constructor có truyền bidTime tường minh.
     *
     * <p>Dùng cho cơ chế AUTO-BID: khi {@link com.auction.model.auction.Auction#processAutoBids(String)}
     * sinh nhiều bid liên tiếp trong cùng một burst, các bid có thể xảy ra
     * cách nhau chỉ vài nano-giây. Để bảo đảm timestamp luôn TĂNG NGHIÊM
     * NGẶT (strictly increasing - mỗi bid sau lớn hơn bid trước), code
     * auto-bid sẽ TỰ tính bidTime rồi truyền vào đây thay vì để
     * {@code LocalDateTime.now()} (vốn có thể trả về cùng giá trị nếu
     * gọi quá nhanh).
     *
     * <p>Lợi ích phụ: tránh duplicate label trên trục X (CategoryAxis) của
     * biểu đồ - nếu hai bid có cùng nhãn giờ, biểu đồ sẽ hiển thị sai.
     *
     * @param auctionId   ID của phiên đấu giá
     * @param bidderId    ID của người đặt giá (auto-bidder)
     * @param bidderName  Tên hiển thị
     * @param bidAmount   Số tiền bid mới
     * @param previousBid Giá trước khi bid
     * @param bidTime     Thời điểm bid (do caller chỉ định, không tự now())
     */
    public BidTransaction(String auctionId, String bidderId, String bidderName,
                          double bidAmount, double previousBid, LocalDateTime bidTime) {
        super();
        this.auctionId   = auctionId;
        this.bidderId    = bidderId;
        this.bidderName  = bidderName;
        this.bidAmount   = bidAmount;
        this.previousBid = previousBid;
        // Dùng thời gian do caller truyền vào (KHÔNG tự lấy now()).
        this.bidTime     = bidTime;
    }

    // =====================================================================
    // CÁC GETTER - chỉ đọc, không có setter (immutable)
    // =====================================================================

    /**
     * @return ID phiên đấu giá mà bid này thuộc về
     */
    public String getAuctionId() {
        return auctionId;
    }

    /**
     * @return ID của người đặt giá
     */
    public String getBidderId() {
        return bidderId;
    }

    /**
     * @return Tên hiển thị của người đặt giá
     */
    public String getBidderName() {
        return bidderName;
    }

    /**
     * @return Số tiền của lần bid này
     */
    public double getBidAmount() {
        return bidAmount;
    }

    /**
     * @return Giá cao nhất trước lần bid này (để tính mức tăng = bidAmount - previousBid)
     */
    public double getPreviousBid() {
        return previousBid;
    }

    /**
     * @return Thời điểm thực hiện bid
     */
    public LocalDateTime getBidTime() {
        return bidTime;
    }

    // =====================================================================
    // PHƯƠNG THỨC ĐA HÌNH - OVERRIDE TỪ ENTITY
    // =====================================================================

    /**
     * In thông tin tóm tắt của giao dịch bid - tiện cho log, debug.
     *
     * <p>Đây là override của phương thức {@code printInfo()} trừu tượng
     * (abstract) hoặc chung chung trong lớp cha {@link Entity}. Mỗi loại
     * entity (User, Auction, BidTransaction...) có cách "tự giới thiệu"
     * riêng - đây chính là tính ĐA HÌNH (polymorphism).
     *
     * @return chuỗi định dạng "BidTransaction[auction=..., bidder=..., amount=..., time=...]"
     */
    @Override
    public String printInfo() {
        return String.format("BidTransaction[auction=%s, bidder=%s, amount=%.2f, time=%s]",
                auctionId, bidderName, bidAmount, bidTime);
    }
}
