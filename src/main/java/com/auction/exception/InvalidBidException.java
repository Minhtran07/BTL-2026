package com.auction.exception;


/**
 * ============================================================================
 *           NGOẠI LỆ: GIÁ ĐẶT (BID) KHÔNG HỢP LỆ
 * ============================================================================
 *
 * <p><b>1. Khi nào ném (throw) ngoại lệ này?</b><br>
 * Khi người dùng gửi một lệnh đặt giá (bid) <b>vi phạm quy tắc nghiệp vụ</b>
 * về giá trị, chẳng hạn:
 * <ul>
 *   <li>Giá đặt <b>thấp hơn hoặc bằng</b> giá hiện tại của phiên
 *       (ví dụ: giá hiện tại 150k, user đặt 100k).</li>
 *   <li>Giá đặt <b>thấp hơn bước giá tối thiểu</b> (bid increment).
 *       Ví dụ: phiên yêu cầu mỗi lần tăng tối thiểu 10k mà user chỉ tăng 5k.</li>
 *   <li>Giá đặt <b>không phải số dương</b> (âm, bằng 0, hoặc NaN).</li>
 *   <li>Giá đặt <b>vượt quá giới hạn</b> hệ thống cho phép (overflow).</li>
 *   <li>User <b>tự đấu giá chính sản phẩm của mình</b> (self-bidding) — nếu
 *       quy tắc dự án cấm.</li>
 *   <li>Số dư tài khoản người dùng <b>không đủ</b> để cam kết khoản đặt giá đó.</li>
 * </ul></p>
 *
 * <p><b>2. Phân biệt với AuctionClosedException:</b><br>
 * <ul>
 *   <li><code>AuctionClosedException</code>: phiên có vấn đề về <b>trạng thái</b>
 *       (đã đóng / hết hạn).</li>
 *   <li><code>InvalidBidException</code>: phiên vẫn mở, nhưng <b>giá trị bid</b>
 *       không hợp lệ.</li>
 * </ul>
 * Trong một lệnh <code>placeBid</code>, ta thường kiểm tra trạng thái phiên
 * <b>TRƯỚC</b>, rồi mới kiểm tra giá — nên thứ tự throw cũng vậy.</p>
 *
 * <p><b>3. Vị trí trong hierarchy:</b>
 * <pre>
 *     Exception
 *       └── AuctionException                 (cha — base)
 *             └── InvalidBidException        (lớp này)
 * </pre>
 * Là <b>CHECKED exception</b> — bắt buộc xử lý ở nơi gọi. Hợp lý vì đây là
 * tình huống <i>cực kỳ phổ biến</i> trong đấu giá và <b>luôn</b> cần phản
 * hồi lại user (không thể "lờ" đi).</p>
 *
 * <p><b>4. Ví dụ tình huống ném trong code:</b>
 * <pre>{@code
 * public void placeBid(int userId, int auctionId, double amount)
 *         throws AuctionClosedException, InvalidBidException {
 *
 *     Auction a = auctionDao.findById(auctionId);
 *
 *     // (a) kiểm tra phiên có còn mở không
 *     if (a.isClosed()) {
 *         throw new AuctionClosedException("Phiên đã đóng");
 *     }
 *
 *     // (b) kiểm tra giá hợp lệ
 *     if (amount <= a.getCurrentPrice()) {
 *         throw new InvalidBidException(
 *             "Giá đặt phải lớn hơn giá hiện tại: " + a.getCurrentPrice());
 *     }
 *     if (amount < a.getCurrentPrice() + a.getMinIncrement()) {
 *         throw new InvalidBidException(
 *             "Phải tăng tối thiểu " + a.getMinIncrement() + " đ");
 *     }
 *
 *     // ... lưu bid vào DB
 * }
 * }</pre></p>
 *
 * <p><b>5. Cách bắt (catch) ở UI:</b>
 * <pre>{@code
 * try {
 *     service.placeBid(uid, aid, amount);
 *     view.showSuccess("Đặt giá thành công!");
 * } catch (InvalidBidException e) {
 *     view.showError(e.getMessage());  // ví dụ: "Phải tăng tối thiểu 10000 đ"
 * } catch (AuctionClosedException e) {
 *     view.showError("Phiên đã kết thúc");
 * }
 * }</pre>
 * Lưu ý: nếu <code>catch</code> theo thứ tự ngược lại (cha trước, con sau)
 * thì compiler sẽ báo lỗi <i>"unreachable"</i> với khối catch sau.</p>
 */
public class InvalidBidException extends AuctionException {

    /**
     * Khởi tạo ngoại lệ "giá đặt không hợp lệ" với thông điệp mô tả lý do
     * cụ thể (tiếng Việt).
     *
     * <p>Thông điệp <b>nên</b> ghi rõ <i>điều kiện hợp lệ</i> để user biết
     * phải sửa thế nào. Ví dụ tốt: "Giá đặt phải lớn hơn 150.000 đ và là
     * bội số của 10.000". Ví dụ kém: chỉ ghi "Giá sai" — user không biết
     * sai ở đâu.</p>
     *
     * <p>Lớp này hiện không cung cấp constructor nhận <code>cause</code>
     * vì lỗi giá không hợp lệ được phát hiện bằng <b>kiểm tra điều kiện
     * thuần túy</b>, không bắt nguồn từ exception khác.</p>
     *
     * @param message mô tả lý do giá đặt không hợp lệ, bằng tiếng Việt,
     *                rõ ràng để hiển thị trực tiếp cho người dùng.
     */
    public InvalidBidException(String message) {
        // Đẩy message qua AuctionException lên Exception để lưu trữ và
        // có thể lấy lại bằng getMessage() ở nơi catch.
        super(message);
    }
}
