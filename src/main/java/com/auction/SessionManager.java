package com.auction;

import com.auction.model.user.User;

/**
 * ============================================================================
 * SESSIONMANAGER - QUẢN LÝ PHIÊN ĐĂNG NHẬP (Client-side)
 * ============================================================================
 *
 * <p>Đây là một <b>Singleton</b> dùng để lưu thông tin người dùng đang đăng
 * nhập trong phiên hiện tại (chỉ ở phía client).
 *
 * <p><b>Singleton Pattern</b> nghĩa là: trong toàn bộ ứng dụng chỉ có DUY NHẤT
 * 1 instance của lớp này. Mọi nơi đều dùng chung một đối tượng. Phù hợp ở đây
 * vì chỉ có 1 user đăng nhập tại 1 thời điểm trên client.
 *
 * <p><b>Tại sao cần?</b> Sau khi user đăng nhập thành công, server trả về
 * thông tin user. Client lưu vào SessionManager. Các màn hình sau (Dashboard,
 * Chi tiết phiên, Tạo item...) chỉ cần gọi {@code SessionManager.getInstance()
 * .getCurrentUser()} để biết user nào đang đăng nhập, không cần truyền qua
 * lại giữa các controller.
 *
 * <p><b>Phân biệt với server-side session:</b> Đây chỉ là cache phía client.
 * Server vẫn xác thực mỗi request riêng. Nếu hacker giả mạo SessionManager,
 * server sẽ phát hiện vì không có session token hợp lệ.
 */
public class SessionManager {

    /**
     * Instance duy nhất của Singleton.
     *
     * <p>Khai báo {@code static} để chỉ có một bản trong cả chương trình.
     * Không khởi tạo ngay (lazy initialization) → tiết kiệm bộ nhớ nếu
     * chưa cần dùng.
     */
    private static SessionManager instance;

    /**
     * User đang đăng nhập hiện tại. {@code null} nếu chưa đăng nhập / đã logout.
     */
    private User currentUser;

    /**
     * Constructor PRIVATE - đặc trưng quan trọng của Singleton.
     *
     * <p>Vì constructor là private nên các lớp khác KHÔNG THỂ gọi
     * {@code new SessionManager()}. Cách duy nhất để lấy instance là gọi
     * {@link #getInstance()}.
     */
    private SessionManager() {}

    /**
     * Lấy instance duy nhất của SessionManager.
     *
     * <p><b>Kỹ thuật Double-Checked Locking (DCL):</b>
     * <ul>
     *   <li>Kiểm tra null 2 lần (1 lần ngoài, 1 lần trong block synchronized)</li>
     *   <li>Lần 1: nếu đã có instance → return ngay (không cần lock - nhanh)</li>
     *   <li>Lần 2 (trong lock): tránh trường hợp 2 thread cùng vượt qua lần 1
     *       rồi tạo 2 instance khác nhau (race condition)</li>
     * </ul>
     *
     * <p>Đây là pattern chuẩn cho Singleton thread-safe trong môi trường đa
     * luồng (như JavaFX có thread UI + thread network).
     *
     * @return instance duy nhất của SessionManager
     */
    public static SessionManager getInstance() {
        // Check 1: nếu instance đã tồn tại, return ngay (không tốn chi phí lock)
        if (instance == null) {
            // Lock cấp lớp - chỉ 1 thread vào được tại 1 thời điểm
            synchronized (SessionManager.class) {
                // Check 2: thread khác có thể đã tạo instance trong lúc chờ lock
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    /**
     * Trả về user đang đăng nhập.
     *
     * @return user hiện tại, hoặc {@code null} nếu chưa đăng nhập
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Đặt user đang đăng nhập (gọi sau khi login thành công).
     *
     * @param user user vừa đăng nhập thành công (server trả về)
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Kiểm tra xem đã đăng nhập hay chưa.
     *
     * @return {@code true} nếu có user trong session, {@code false} nếu chưa
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Đăng xuất - xóa user khỏi session phía client.
     *
     * <p>Lưu ý: phương thức này chỉ xóa cache client. Để logout hoàn toàn,
     * controller phải gọi thêm request LOGOUT lên server (xem
     * {@code LoginController#handleLogout}).
     */
    public void logout() {
        this.currentUser = null;
    }
}
