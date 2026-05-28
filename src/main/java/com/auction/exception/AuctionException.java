package com.auction.exception;


/**
 * ============================================================================
 *                  LỚP NGOẠI LỆ CƠ SỞ CHO HỆ THỐNG ĐẤU GIÁ
 * ============================================================================
 *
 * <p><b>1. Mục đích:</b><br>
 * Đây là lớp ngoại lệ (exception) <b>CHA</b> (base class) cho toàn bộ các loại
 * lỗi nghiệp vụ (business errors) trong hệ thống đấu giá online client-server.
 * Mọi exception đặc thù của domain đấu giá (đặt giá sai, phiên đã đóng, sai
 * mật khẩu...) đều <b>kế thừa (extends)</b> lớp này. Nhờ vậy, ta có thể bắt
 * tất cả lỗi nghiệp vụ chỉ bằng <code>catch (AuctionException e)</code>.</p>
 *
 * <p><b>2. Vì sao cần một base exception riêng?</b><br>
 * Trong OOP, việc gom các exception cùng "họ" vào một cây thừa kế giúp:
 * <ul>
 *   <li>Code <b>polymorphism</b>: 1 khối catch xử lý được nhiều loại lỗi.</li>
 *   <li>Tách biệt lỗi domain đấu giá với các lỗi hệ thống khác (IO, SQL...).</li>
 *   <li>Dễ mở rộng: thêm exception mới chỉ cần <code>extends AuctionException</code>.</li>
 * </ul></p>
 *
 * <p><b>3. Checked hay Unchecked?</b><br>
 * Lớp này <b>CHECKED</b> (kế thừa trực tiếp {@link Exception}, KHÔNG phải
 * {@link RuntimeException}). Hệ quả:
 * <ul>
 *   <li>Mọi method ném ra <code>AuctionException</code> <b>BẮT BUỘC</b> khai báo
 *       <code>throws AuctionException</code> trong chữ ký, hoặc phải try-catch.</li>
 *   <li>Compiler sẽ báo lỗi nếu lập trình viên "lờ" đi không xử lý — đây là
 *       <b>cố ý</b> vì các lỗi nghiệp vụ (như đặt giá sai) là tình huống
 *       <i>có thể dự đoán và phải xử lý</i> (ví dụ: hiện thông báo cho user).</li>
 * </ul>
 * Lưu ý: {@link DataAccessException} thì lại là <b>UNCHECKED</b> vì lỗi DB
 * thường không thể phục hồi tại chỗ — chi tiết xem trong file đó.</p>
 *
 * <p><b>4. Cây thừa kế (hierarchy) hiện tại:</b><br>
 * <pre>
 *     java.lang.Exception
 *         └── AuctionException                 (lớp này — gốc của domain)
 *               ├── AuctionClosedException     (đặt giá vào phiên đã đóng)
 *               ├── AuthenticationException    (đăng nhập sai)
 *               └── InvalidBidException        (giá đặt &lt;= giá hiện tại)
 *
 *     java.lang.RuntimeException
 *         └── DataAccessException              (lỗi DB — unchecked, đứng riêng)
 * </pre></p>
 *
 * <p><b>5. Ví dụ tình huống ném exception trong dự án:</b>
 * <ul>
 *   <li>User A muốn đặt giá nhưng phiên đấu giá đã hết thời gian
 *       → ném <code>AuctionClosedException</code>.</li>
 *   <li>User nhập sai mật khẩu khi đăng nhập
 *       → ném <code>AuthenticationException</code>.</li>
 *   <li>User đặt giá 100.000đ nhưng giá hiện tại đã là 150.000đ
 *       → ném <code>InvalidBidException</code>.</li>
 *   <li>Mất kết nối tới MySQL khi truy vấn
 *       → ném <code>DataAccessException</code>.</li>
 * </ul></p>
 *
 * <p><b>6. Mẫu sử dụng (pattern) ở tầng service / controller:</b>
 * <pre>{@code
 * try {
 *     auctionService.placeBid(userId, auctionId, amount);
 * } catch (AuctionClosedException e) {
 *     // Báo "Phiên đã đóng"
 * } catch (InvalidBidException e) {
 *     // Báo "Giá đặt không hợp lệ"
 * } catch (AuctionException e) {
 *     // Bắt mọi lỗi nghiệp vụ khác chưa lường trước (fallback)
 * }
 * }</pre>
 * Thứ tự catch: <b>luôn bắt class con TRƯỚC class cha</b>, nếu không Java
 * compiler sẽ báo "unreachable code".</p>
 *
 * @author Nhóm BTL Đấu Giá
 */
public class AuctionException extends Exception {

    /**
     * Khởi tạo ngoại lệ chỉ với <b>thông điệp mô tả lỗi</b>.
     *
     * <p>Dùng khi: lỗi nghiệp vụ phát sinh nhưng <b>không có exception gốc</b>
     * nào gây ra (ví dụ: tự kiểm tra điều kiện rồi <code>throw new ...</code>).</p>
     *
     * <p>Ví dụ:
     * <pre>{@code
     * if (bidAmount <= currentPrice) {
     *     throw new InvalidBidException("Giá đặt phải cao hơn giá hiện tại");
     * }
     * }</pre></p>
     *
     * @param message thông điệp mô tả lỗi — nên viết bằng tiếng Việt rõ ràng
     *                để hiển thị cho người dùng cuối hoặc ghi log.
     */
    public AuctionException(String message) {
        // Gọi constructor của lớp cha (java.lang.Exception) để lưu message.
        // 'super' là từ khóa tham chiếu tới lớp cha trong OOP.
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với <b>thông điệp</b> và <b>nguyên nhân gốc (cause)</b>.
     *
     * <p>Dùng khi: ta <i>bắt</i> được một exception khác (ví dụ {@link java.sql.SQLException})
     * và muốn <b>"bọc" (wrap)</b> nó thành <code>AuctionException</code> để
     * trả về tầng cao hơn, nhưng vẫn <b>giữ lại stack trace gốc</b> phục vụ
     * debug.</p>
     *
     * <p>Đây gọi là kỹ thuật <b>"exception chaining"</b> — rất quan trọng trong
     * dự án nhiều tầng vì giúp truy ngược về dòng code gây lỗi thực sự.</p>
     *
     * <p>Ví dụ:
     * <pre>{@code
     * try {
     *     // truy vấn DB
     * } catch (SQLException ex) {
     *     throw new AuctionException("Không thể tải phiên đấu giá", ex);
     * }
     * }</pre></p>
     *
     * @param message thông điệp mô tả lỗi (tiếng Việt, dành cho user/log).
     * @param cause   exception gốc gây ra lỗi này (để giữ stack trace);
     *                có thể là <code>null</code> nếu chưa biết.
     */
    public AuctionException(String message, Throwable cause) {
        // Truyền cả message lẫn cause lên lớp cha — Java sẽ tự in cả 2 khi
        // gọi printStackTrace(), kèm dòng "Caused by: ..." rất hữu ích.
        super(message, cause);
    }
}
