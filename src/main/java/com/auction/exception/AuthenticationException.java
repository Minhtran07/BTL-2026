package com.auction.exception;


/**
 * ============================================================================
 *           NGOẠI LỆ: XÁC THỰC NGƯỜI DÙNG THẤT BẠI
 * ============================================================================
 *
 * <p><b>1. Khi nào ném (throw) ngoại lệ này?</b><br>
 * Khi quá trình <b>xác thực (authentication)</b> không thành công, tức là
 * hệ thống <b>không xác định được</b> người dùng là ai hoặc không thể chứng
 * minh họ là chủ sở hữu tài khoản. Các tình huống điển hình:
 * <ul>
 *   <li>Sai tên đăng nhập (username/email không tồn tại trong DB).</li>
 *   <li>Sai mật khẩu (hash mật khẩu không khớp).</li>
 *   <li>Tài khoản bị khóa / bị admin vô hiệu hóa.</li>
 *   <li>Token / session đã hết hạn khi client gửi request đấu giá.</li>
 *   <li>Tài khoản chưa xác minh email (nếu hệ thống yêu cầu).</li>
 * </ul></p>
 *
 * <p><b>2. Phân biệt với Authorization (phân quyền):</b><br>
 * Hai khái niệm dễ nhầm:
 * <ul>
 *   <li><b>Authentication</b> — <i>"Bạn là ai?"</i> → dùng exception này.</li>
 *   <li><b>Authorization</b> — <i>"Bạn có quyền làm điều này không?"</i>
 *       → nên dùng exception khác (ví dụ <code>AccessDeniedException</code>);
 *       hiện dự án chưa có, có thể bổ sung sau.</li>
 * </ul>
 * Đừng dùng <code>AuthenticationException</code> cho lỗi phân quyền — sẽ
 * gây hiểu nhầm và khó bảo trì.</p>
 *
 * <p><b>3. Vị trí trong hierarchy (cây thừa kế):</b>
 * <pre>
 *     Exception
 *       └── AuctionException                 (cha — base)
 *             └── AuthenticationException    (lớp này)
 * </pre>
 * Là <b>CHECKED exception</b> (do kế thừa <code>AuctionException</code>).
 * Buộc lập trình viên phải xử lý đăng nhập sai một cách tường minh —
 * hợp lý vì đây là tình huống <i>rất hay xảy ra</i> và cần phản hồi cho user.</p>
 *
 * <p><b>4. Lưu ý bảo mật khi viết message:</b><br>
 * <b>KHÔNG</b> tiết lộ chi tiết kiểu "Sai mật khẩu" hay "Username không tồn tại"
 * trong thông điệp gửi về client (hacker có thể dùng để dò user). Thay vào đó,
 * trả về thông điệp chung như <i>"Sai tên đăng nhập hoặc mật khẩu"</i>.
 * Chi tiết thật chỉ nên ghi vào log server.</p>
 *
 * <p><b>5. Ví dụ tình huống ném trong code:</b>
 * <pre>{@code
 * public User login(String username, String password)
 *         throws AuthenticationException {
 *
 *     User u = userDao.findByUsername(username);
 *     if (u == null || !PasswordUtils.match(password, u.getHash())) {
 *         throw new AuthenticationException(
 *             "Sai tên đăng nhập hoặc mật khẩu");
 *     }
 *     return u;
 * }
 * }</pre></p>
 *
 * <p><b>6. Cách bắt (catch) ở UI client:</b>
 * <pre>{@code
 * try {
 *     User u = authService.login(name, pass);
 *     openMainWindow(u);
 * } catch (AuthenticationException e) {
 *     loginView.showError(e.getMessage());
 * }
 * }</pre></p>
 */
public class AuthenticationException extends AuctionException {

    /**
     * Khởi tạo ngoại lệ xác thực với thông điệp mô tả lý do thất bại.
     *
     * <p>Như đã lưu ý ở mục bảo mật phía trên: thông điệp này nếu hiển thị
     * cho người dùng cuối thì <b>nên mơ hồ</b> (kiểu "Sai tài khoản hoặc
     * mật khẩu"), còn chi tiết cụ thể (ví dụ "user bị khóa do nhập sai 5
     * lần") chỉ nên xuất hiện trong log nội bộ.</p>
     *
     * @param message thông điệp tiếng Việt mô tả lỗi xác thực. Ví dụ:
     *                <i>"Sai tên đăng nhập hoặc mật khẩu"</i>,
     *                <i>"Phiên đăng nhập đã hết hạn"</i>.
     */
    public AuthenticationException(String message) {
        // Đẩy message lên AuctionException → Exception để lưu trữ.
        // Khi gọi getMessage() sau này, ta sẽ nhận lại đúng chuỗi này.
        super(message);
    }
}
