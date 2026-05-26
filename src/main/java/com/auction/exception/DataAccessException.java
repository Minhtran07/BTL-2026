package com.auction.exception;


/**
 * ============================================================================
 *        NGOẠI LỆ: TRUY CẬP DỮ LIỆU (DATABASE / PERSISTENCE LAYER)
 * ============================================================================
 *
 * <p><b>1. Khi nào ném (throw) ngoại lệ này?</b><br>
 * Khi <b>tầng DAO</b> (Data Access Object — tầng giao tiếp với CSDL) gặp
 * sự cố trong lúc đọc/ghi dữ liệu. Các tình huống điển hình:
 * <ul>
 *   <li>Không kết nối được tới MySQL/PostgreSQL (sai host, sai port, DB tắt).</li>
 *   <li>Câu lệnh SQL sai cú pháp hoặc tham chiếu cột/bảng không tồn tại.</li>
 *   <li>Vi phạm ràng buộc (constraint violation): trùng khóa chính, foreign key sai...</li>
 *   <li>Timeout khi query mất quá lâu.</li>
 *   <li>Mất kết nối giữa chừng (network issue).</li>
 *   <li>Tài nguyên cạn kiệt: connection pool đầy, hết bộ nhớ...</li>
 * </ul></p>
 *
 * <p><b>2. ĐIỂM ĐẶC BIỆT — đây là UNCHECKED exception!</b><br>
 * Khác với các exception khác trong package (đều kế thừa
 * <code>AuctionException</code> → checked), lớp này kế thừa thẳng
 * {@link RuntimeException}, nên là <b>UNCHECKED</b>. Hệ quả:
 * <ul>
 *   <li><b>KHÔNG</b> bắt buộc khai báo <code>throws</code>.</li>
 *   <li><b>KHÔNG</b> bắt buộc try-catch — compiler không báo lỗi nếu "lờ" đi.</li>
 * </ul>
 *
 * <b>Lý do thiết kế như vậy:</b>
 * <ul>
 *   <li>Lỗi DB hầu hết là <b>không phục hồi được</b> tại tầng nghiệp vụ
 *       (service không biết phải làm gì khi DB sập — chỉ có thể log rồi
 *       báo lỗi chung cho user).</li>
 *   <li>Nếu là checked, mọi method DAO sẽ phải khai báo
 *       <code>throws DataAccessException</code> → "ô nhiễm" chữ ký, lan
 *       <code>throws</code> khắp codebase rất phiền.</li>
 *   <li>Đây là <b>pattern phổ biến</b> trong industry — Spring Framework,
 *       Hibernate đều dùng <code>DataAccessException</code> dạng unchecked.</li>
 * </ul></p>
 *
 * <p><b>3. Vị trí trong hierarchy:</b>
 * <pre>
 *     RuntimeException                          (unchecked branch)
 *       └── DataAccessException                 (lớp này — ĐỨNG RIÊNG)
 *
 *     Exception                                 (checked branch)
 *       └── AuctionException
 *             ├── AuctionClosedException
 *             ├── AuthenticationException
 *             └── InvalidBidException
 * </pre>
 * Lưu ý: <code>DataAccessException</code> <b>KHÔNG</b> kế thừa
 * <code>AuctionException</code> — vì khái niệm "lỗi DB" mang tính hạ tầng
 * (infrastructure), không phải lỗi nghiệp vụ đấu giá.</p>
 *
 * <p><b>4. Mẫu sử dụng ở DAO:</b>
 * <pre>{@code
 * public User findById(int id) {
 *     try (PreparedStatement ps = conn.prepareStatement(SQL)) {
 *         ps.setInt(1, id);
 *         ResultSet rs = ps.executeQuery();
 *         // ... map sang User
 *     } catch (SQLException ex) {
 *         // Bọc SQLException (checked) thành DataAccessException (unchecked)
 *         throw new DataAccessException("Lỗi truy vấn user id=" + id, ex);
 *     }
 * }
 * }</pre></p>
 *
 * <p><b>5. Mẫu xử lý ở tầng service / controller:</b><br>
 * Vì là unchecked, ta thường <b>không catch</b> ở mỗi chỗ gọi DAO, mà chỉ
 * catch <b>tập trung</b> ở một nơi (global handler hoặc top-level controller):
 * <pre>{@code
 * try {
 *     auctionService.placeBid(...);
 * } catch (DataAccessException e) {
 *     log.error("DB error", e);
 *     view.showError("Hệ thống đang gặp sự cố, vui lòng thử lại sau");
 * }
 * }</pre></p>
 */
public class DataAccessException extends RuntimeException {

    /**
     * Khởi tạo ngoại lệ truy cập dữ liệu với <b>thông điệp mô tả lỗi</b>.
     *
     * <p>Dùng khi: ta tự phát hiện ra tình huống dữ liệu bất thường mà
     * <b>không có exception gốc</b> (ví dụ: kiểm tra điều kiện rồi tự
     * <code>throw</code>).</p>
     *
     * <p>Ví dụ:
     * <pre>{@code
     * if (rs.next() == false) {
     *     throw new DataAccessException("Không tìm thấy bản ghi nào");
     * }
     * }</pre></p>
     *
     * @param message mô tả ngắn gọn vì sao thao tác DB thất bại (tiếng Việt).
     */
    public DataAccessException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với <b>thông điệp</b> và <b>nguyên nhân gốc</b>.
     *
     * <p>Đây là constructor <b>thường dùng nhất</b> trong DAO: ta bắt
     * {@link java.sql.SQLException} (checked) rồi <b>bọc (wrap)</b> nó
     * thành <code>DataAccessException</code> (unchecked) để truyền lên các
     * tầng trên mà không cần khai báo <code>throws</code>.</p>
     *
     * <p>Việc giữ lại <code>cause</code> rất quan trọng: khi log,
     * Java sẽ in <i>"Caused by: java.sql.SQLException: ..."</i> bên dưới
     * stack trace chính, giúp developer truy ngược được câu SQL gốc gây lỗi.</p>
     *
     * <p>Ví dụ:
     * <pre>{@code
     * try {
     *     stmt.executeUpdate();
     * } catch (SQLException ex) {
     *     throw new DataAccessException("Không thể lưu bid vào DB", ex);
     * }
     * }</pre></p>
     *
     * @param message mô tả lỗi (tiếng Việt) để hiển thị / ghi log.
     * @param cause   exception gốc (thường là {@link java.sql.SQLException}).
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
