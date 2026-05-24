package com.auction.service;

import com.auction.dao.UserDao;
import com.auction.exception.AuthenticationException;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * USERSERVICETEST - UNIT TEST CHO USERSERVICE
 * ============================================================================
 *
 * <p>Test các nghiệp vụ user:
 * <ul>
 *   <li>register(): validate + tạo Bidder/Seller/Admin theo role</li>
 *   <li>login(): xác thực password (hashed)</li>
 *   <li>deactivateUser(): khóa tài khoản</li>
 *   <li>findByUsername(), findById(), findAll()</li>
 * </ul>
 *
 * <p><b>Cách ly hoàn toàn</b>: mỗi test dùng {@link InMemoryUserDao} — không đọc
 * hoặc ghi bất kỳ file nào trên đĩa.  Không còn phụ thuộc vào {@code data/users.dat}.
 */
class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        // Mỗi test nhận một UserService với DAO sạch, hoàn toàn in-memory
        userService = new UserService(new InMemoryUserDao());
    }

    // ==================== Registration ====================

    @Test
    @DisplayName("Đăng ký thành công với thông tin hợp lệ")
    void testRegisterSuccess() throws AuthenticationException {
        User user = userService.register("alice", "pass1234",
                "alice@test.com", "Alice Nguyen", UserRole.BIDDER);

        assertNotNull(user);
        assertEquals("alice", user.getUsername());
        assertEquals(UserRole.BIDDER, user.getRole());
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("Mật khẩu phải được băm - không lưu plaintext")
    void testPasswordIsHashed() throws AuthenticationException {
        User user = userService.register("bob", "mySecret123",
                "bob@test.com", "Bob Tran", UserRole.BIDDER);

        // Mật khẩu lưu trong DB không được bằng plaintext
        assertNotEquals("mySecret123", user.getPassword(),
                "Mật khẩu phải được băm trước khi lưu");
        // Phải xác thực được với plaintext gốc
        assertTrue(user.validatePassword("mySecret123"),
                "validatePassword phải trả về true với mật khẩu đúng");
        assertFalse(user.validatePassword("wrongPassword"),
                "validatePassword phải trả về false với mật khẩu sai");
    }

    @Test
    @DisplayName("Đăng ký thất bại - username rỗng")
    void testRegisterEmptyUsername() {
        assertThrows(AuthenticationException.class, () ->
                userService.register("", "pass123", "test@test.com", "Test", UserRole.BIDDER));
    }

    @Test
    @DisplayName("Đăng ký thất bại - mật khẩu quá ngắn")
    void testRegisterShortPassword() {
        assertThrows(AuthenticationException.class, () ->
                userService.register("user123", "abc", "test@test.com", "Test", UserRole.BIDDER));
    }

    @Test
    @DisplayName("Đăng ký thất bại - email không hợp lệ")
    void testRegisterInvalidEmail() {
        assertThrows(AuthenticationException.class, () ->
                userService.register("user123", "pass123", "invalid-email", "Test", UserRole.BIDDER));
    }

    @Test
    @DisplayName("Đăng ký thất bại - username đã tồn tại")
    void testRegisterDuplicateUsername() throws AuthenticationException {
        userService.register("charlie", "pass1234", "charlie@test.com", "Charlie", UserRole.BIDDER);

        assertThrows(AuthenticationException.class, () ->
                userService.register("charlie", "other1234", "other@test.com", "Other", UserRole.BIDDER));
    }

    @Test
    @DisplayName("Đăng ký thất bại - email đã được dùng")
    void testRegisterDuplicateEmail() throws AuthenticationException {
        userService.register("dave", "pass1234", "shared@test.com", "Dave", UserRole.BIDDER);

        assertThrows(AuthenticationException.class, () ->
                userService.register("eve", "pass1234", "shared@test.com", "Eve", UserRole.SELLER));
    }

    @Test
    @DisplayName("Đăng ký Seller - vai trò đúng")
    void testRegisterSeller() throws AuthenticationException {
        User user = userService.register("seller1", "pass1234",
                "seller1@test.com", "Seller User", UserRole.SELLER);

        assertEquals(UserRole.SELLER, user.getRole());
    }

    // ==================== Login ====================

    @Test
    @DisplayName("Đăng nhập thành công")
    void testLoginSuccess() throws AuthenticationException {
        userService.register("frank", "pass1234", "frank@test.com", "Frank", UserRole.BIDDER);

        User user = userService.login("frank", "pass1234");
        assertNotNull(user);
        assertEquals("frank", user.getUsername());
    }

    @Test
    @DisplayName("Đăng nhập thất bại - sai mật khẩu")
    void testLoginWrongPassword() throws AuthenticationException {
        userService.register("grace", "pass1234", "grace@test.com", "Grace", UserRole.BIDDER);

        assertThrows(AuthenticationException.class, () ->
                userService.login("grace", "wrongpassword"));
    }

    @Test
    @DisplayName("Đăng nhập thất bại - tài khoản không tồn tại")
    void testLoginNonExistent() {
        assertThrows(AuthenticationException.class, () ->
                userService.login("nobody_here", "pass123"));
    }

    @Test
    @DisplayName("Đăng nhập thất bại - null input")
    void testLoginNullInput() {
        assertThrows(AuthenticationException.class, () ->
                userService.login(null, null));
    }

    // ==================== Deactivate ====================

    @Test
    @DisplayName("Vô hiệu hóa tài khoản thành công")
    void testDeactivateUser() throws AuthenticationException {
        User user = userService.register("henry", "pass1234",
                "henry@test.com", "Henry", UserRole.BIDDER);

        assertTrue(user.isActive());
        userService.deactivateUser(user.getId());

        // Đăng nhập lại phải thất bại
        assertThrows(AuthenticationException.class, () ->
                userService.login("henry", "pass1234"));
    }

    // ================================================================
    //  In-memory UserDao — không đọc/ghi file, hoàn toàn isolated
    // ================================================================

    /**
     * Triển khai UserDao in-memory cho mục đích test.
     * Không có phụ thuộc vào file system.
     */
    private static class InMemoryUserDao implements UserDao {

        private final Map<String, User> store = new ConcurrentHashMap<>();

        @Override
        public void save(User entity) {
            store.put(entity.getId(), entity);
        }

        @Override
        public Optional<User> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public void update(User entity) {
            store.put(entity.getId(), entity);
        }

        @Override
        public void delete(String id) {
            store.remove(id);
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return store.values().stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username))
                    .findFirst();
        }

        @Override
        public boolean existsByUsername(String username) {
            return store.values().stream()
                    .anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
        }

        @Override
        public boolean existsByEmail(String email) {
            return store.values().stream()
                    .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        }
    }
}
