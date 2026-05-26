package com.auction.exception;


/**
 * ============================================================================
 *           NGOẠI LỆ: ĐẶT GIÁ VÀO PHIÊN ĐẤU GIÁ ĐÃ ĐÓNG
 * ============================================================================
 *
 * <p><b>1. Khi nào ném (throw) ngoại lệ này?</b><br>
 * Khi người dùng cố gắng thực hiện một hành động (thường là <i>đặt giá</i>)
 * trên một phiên đấu giá mà phiên đó <b>không còn hoạt động</b>:
 * <ul>
 *   <li>Phiên đã hết thời gian (end-time &lt; now).</li>
 *   <li>Phiên bị admin đóng / hủy thủ công.</li>
 *   <li>Phiên đã có người mua bằng giá "Mua ngay" (buy-now) — nếu hệ thống có.</li>
 *   <li>Phiên đang ở trạng thái "đã kết thúc" trong DB (status = CLOSED).</li>
 * </ul></p>
 *
 * <p><b>2. Tại sao cần exception riêng (không dùng AuctionException chung)?</b><br>
 * Đây là một <b>tình huống nghiệp vụ rất cụ thể</b>, cần phản hồi cho user
 * thông điệp đặc thù như "Phiên đã đóng, vui lòng chọn phiên khác". Khi
 * tách thành một class riêng:
 * <ul>
 *   <li>Tầng controller / UI có thể <code>catch</code> riêng và hiển thị giao diện phù hợp
 *       (ví dụ: chuyển hướng về trang chủ).</li>
 *   <li>Code dễ đọc hơn — tên class chính là tài liệu (self-documenting).</li>
 *   <li>Là minh họa tốt cho nguyên tắc <b>Single Responsibility</b> trong OOP.</li>
 * </ul></p>
 *
 * <p><b>3. Vị trí trong hierarchy (cây thừa kế):</b>
 * <pre>
 *     Exception
 *       └── AuctionException                 (cha — base)
 *             └── AuctionClosedException     (lớp này)
 * </pre>
 * Vì kế thừa <code>AuctionException</code> (vốn là <b>checked</b>), lớp này
 * cũng là <b>CHECKED exception</b> — nơi gọi BẮT BUỘC phải try-catch hoặc
 * khai báo <code>throws</code>.</p>
 *
 * <p><b>4. Ví dụ tình huống ném trong code:</b>
 * <pre>{@code
 * public void placeBid(int userId, int auctionId, double amount)
 *         throws AuctionClosedException, InvalidBidException {
 *
 *     Auction a = auctionDao.findById(auctionId);
 *     if (a.getStatus() == Status.CLOSED || a.getEndTime().isBefore(now())) {
 *         throw new AuctionClosedException(
 *             "Phiên #" + auctionId + " đã đóng, không thể đặt giá");
 *     }
 *     // ... tiếp tục đặt giá
 * }
 * }</pre></p>
 *
 * <p><b>5. Cách bắt (catch) ở tầng trên:</b>
 * <pre>{@code
 * try {
 *     service.placeBid(uid, aid, money);
 * } catch (AuctionClosedException e) {
 *     view.showError("Phiên đấu giá đã kết thúc!");
 * }
 * }</pre></p>
 */
public class AuctionClosedException extends AuctionException {

    /**
     * Khởi tạo ngoại lệ với thông điệp mô tả lý do phiên bị coi là "đã đóng".
     *
     * <p>Thông điệp này thường được hiển thị trực tiếp cho người dùng hoặc
     * ghi vào log của server, nên cần viết <b>rõ ràng, ngắn gọn, bằng tiếng
     * Việt</b>. Có thể chèn thêm ID phiên, thời điểm đóng để dễ truy vết.</p>
     *
     * <p>Lưu ý: lớp này <b>không có</b> constructor nhận <code>cause</code>
     * vì tình huống "phiên đã đóng" thường được phát hiện bằng cách
     * <i>kiểm tra logic</i> (so sánh thời gian, trạng thái), <b>không bắt nguồn
     * từ exception khác</b>. Nếu sau này cần wrap exception gốc, có thể bổ
     * sung constructor tương tự bên <code>AuctionException</code>.</p>
     *
     * @param message thông điệp tiếng Việt mô tả vì sao phiên đã đóng,
     *                ví dụ: <i>"Phiên #42 đã hết hạn lúc 22:00"</i>.
     */
    public AuctionClosedException(String message) {
        // Chuyển message lên cho lớp cha AuctionException xử lý lưu trữ.
        // Lớp cha lại tiếp tục gọi super(message) lên java.lang.Exception.
        super(message);
    }
}
