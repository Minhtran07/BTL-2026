package com.auction.service;

import com.auction.dao.UserDao;
import com.auction.dao.UserDaoImpl;
import com.auction.exception.AuthenticationException;
import com.auction.model.user.*;
import com.auction.util.PasswordUtils;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * USERSERVICE - LỚP NGHIỆP VỤ NGƯỜI DÙNG (BUSINESS LOGIC LAYER)
 * ============================================================================
 *
 * <p>Đây là tầng SERVICE - nằm giữa Controller (UI) và DAO (database).
 * Vai trò:
 * <ul>
 *   <li>Xử lý logic nghiệp vụ (đăng ký, đăng nhập, validate dữ liệu...)</li>
 *   <li>Gọi DAO để CRUD dữ liệu</li>
 *   <li>Băm mật khẩu trước khi lưu</li>
 *   <li>Ném exception phù hợp khi có lỗi</li>
 * </ul>
 *
 * <p><b>TẠI SAO CẦN SERVICE LAYER?</b>
 * <ul>
 *   <li>Tách biệt logic nghiệp vụ khỏi UI và DB</li>
 *   <li>Tái sử dụng: nhiều Controller có thể gọi cùng 1 service</li>
 *   <li>Dễ test: mock DAO để test service riêng</li>
 *   <li>Dễ thay đổi: đổi DB → chỉ sửa DAO, service không đổi</li>
 * </ul>
 *
 * <p><b>DEPENDENCY INJECTION:</b> Constructor có 2 phiên bản - một mặc định
 * (tự tạo DAO) và một nhận DAO làm tham số (cho unit test). Đây là kỹ thuật
 * "constructor injection" - cho phép thay thế dependency mà không sửa code.
 */
public class UserService {

    /** DAO truy cập DB user. Khai báo interface (không phải impl) → DI. */
    private final UserDao userDao;

    /**
     * Constructor mặc định - dùng trong production.
     * Tự tạo UserDaoImpl (SQLite).
     */
    public UserService() {
        this.userDao = new UserDaoImpl();
    }

    /**
     * Constructor cho test - cho phép truyền DAO mock từ ngoài.
     * Đây là dependency injection - giúp test isolated khỏi database thật.
     */
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Đăng ký tài khoản mới.
     *
     * <p><b>Các bước:</b>
     * <ol>
     *   <li>Validate input (username, password, email)</li>
     *   <li>Kiểm tra trùng username, email</li>
     *   <li>Băm mật khẩu (KHÔNG lưu plaintext)</li>
     *   <li>Tạo đúng loại User theo role (Bidder/Seller/Admin)</li>
     *   <li>Save vào DB</li>
     * </ol>
     *
     * @param username tên đăng nhập
     * @param password mật khẩu PLAINTEXT (sẽ được hash)
     * @param email    email
     * @param fullName họ tên đầy đủ
     * @param role     vai trò (BIDDER/SELLER/ADMIN)
     * @return User vừa tạo (đã có id, hashed password)
     * @throws AuthenticationException nếu validate fail hoặc trùng
     */
    public User register(String username, String password, String email,
                         String fullName, UserRole role) throws AuthenticationException {
        // ===== VALIDATE INPUT =====
        if (username == null || username.trim().isEmpty()) {
            throw new AuthenticationException("Tên đăng nhập không được để trống");
        }
        if (password == null || password.length() < 4) {
            throw new AuthenticationException("Mật khẩu phải có ít nhất 4 ký tự");
        }
        if (email == null || !email.contains("@")) {
            // Validate email đơn giản - production nên dùng regex chuẩn
            throw new AuthenticationException("Email không hợp lệ");
        }

        // ===== KIỂM TRA UNIQUE =====
        if (userDao.existsByUsername(username)) {
            throw new AuthenticationException("Tên đăng nhập đã tồn tại: " + username);
        }
        if (userDao.existsByEmail(email)) {
            throw new AuthenticationException("Email đã được sử dụng: " + email);
        }

        // ===== HASH PASSWORD =====
        // QUAN TRỌNG: luôn hash trước khi lưu, KHÔNG BAO GIỜ lưu plaintext
        String hashedPassword = PasswordUtils.hash(password);

        // ===== TẠO ĐÚNG LOẠI USER THEO ROLE =====
        // Dùng switch expression (Java 14+) - trả về User
        User user = switch (role) {
            case BIDDER -> new Bidder(username, hashedPassword, email, fullName);
            case SELLER -> new Seller(username, hashedPassword, email, fullName);
            case ADMIN  -> new Admin(username, hashedPassword, email, fullName);
        };

        // ===== SAVE VÀO DB =====
        userDao.save(user);
        return user;
    }

    /**
     * Đăng nhập.
     *
     * <p><b>Các bước:</b>
     * <ol>
     *   <li>Validate input không null</li>
     *   <li>Tìm user theo username</li>
     *   <li>So sánh password đã hash</li>
     *   <li>Kiểm tra tài khoản chưa bị khóa</li>
     * </ol>
     *
     * <p><b>Lưu ý bảo mật:</b> Production nên dùng message chung
     * "Sai tài khoản hoặc mật khẩu" để tránh tiết lộ username có tồn tại không.
     *
     * @return User đã đăng nhập thành công
     * @throws AuthenticationException nếu fail bất kỳ bước nào
     */
    public User login(String username, String password) throws AuthenticationException {
        if (username == null || password == null) {
            throw new AuthenticationException("Tên đăng nhập và mật khẩu không được để trống");
        }

        // Tìm user
        Optional<User> userOpt = userDao.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Tài khoản không tồn tại: " + username);
        }

        User user = userOpt.get();
        // So sánh password đã hash (constant-time compare để chống timing attack)
        if (!user.validatePassword(password)) {
            throw new AuthenticationException("Mật khẩu không chính xác");
        }

        // Tài khoản bị admin khóa
        if (!user.isActive()) {
            throw new AuthenticationException("Tài khoản đã bị vô hiệu hóa");
        }

        return user;
    }

    // ===== CÁC METHOD HELPER ĐƠN GIẢN =====

    public Optional<User> findById(String id) {
        return userDao.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userDao.findByUsername(username);
    }

    public List<User> findAll() {
        return userDao.findAll();
    }

    public void updateUser(User user) {
        userDao.update(user);
    }

    public void deleteUser(String id) {
        userDao.delete(id);
    }

    /**
     * Admin vô hiệu hóa tài khoản user khác.
     *
     * <p>Set active = false → user không thể đăng nhập nữa.
     * KHÔNG xóa khỏi DB → giữ lại dữ liệu lịch sử (bid, sale...).
     *
     * @param userId id user cần deactivate
     * @throws AuthenticationException nếu không tìm thấy user
     */
    public void deactivateUser(String userId) throws AuthenticationException {
        Optional<User> userOpt = userDao.findById(userId);
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Không tìm thấy người dùng: " + userId);
        }
        User user = userOpt.get();
        user.setActive(false);
        userDao.update(user);
    }
}
