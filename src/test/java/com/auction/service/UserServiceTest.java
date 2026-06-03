package com.auction.service;

import com.auction.dao.UserDao;
import com.auction.exception.AuthenticationException;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
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

    // ==================== Deduct Bidder Balance ====================

    @Test
    @DisplayName("Trừ tiền Bidder thành công khi đủ số dư")
    void testDeductBidderBalance_Success() throws AuthenticationException {
        User user = userService.register("bidder1", "pass1234",
                "bidder1@test.com", "Bidder One", UserRole.BIDDER);
        Bidder bidder = (Bidder) user;
        double initialBalance = bidder.getBalance();

        boolean result = userService.deductBidderBalance(user.getId(), 500_000);

        assertTrue(result, "Trừ tiền phải thành công khi đủ số dư");
        // Lấy lại từ DAO để xác nhận đã persist
        Bidder updated = (Bidder) userService.findById(user.getId()).orElseThrow();
        assertEquals(initialBalance - 500_000, updated.getBalance(), 0.01,
                "Số dư phải giảm đúng số tiền đã trừ");
    }

    @Test
    @DisplayName("Trừ tiền Bidder thất bại khi không đủ số dư")
    void testDeductBidderBalance_InsufficientFunds() throws AuthenticationException {
        User user = userService.register("bidder2", "pass1234",
                "bidder2@test.com", "Bidder Two", UserRole.BIDDER);
        Bidder bidder = (Bidder) user;
        double initialBalance = bidder.getBalance();

        // Trừ nhiều hơn số dư
        boolean result = userService.deductBidderBalance(user.getId(), initialBalance + 1);

        assertFalse(result, "Trừ tiền phải thất bại khi không đủ số dư");
        Bidder updated = (Bidder) userService.findById(user.getId()).orElseThrow();
        assertEquals(initialBalance, updated.getBalance(), 0.01,
                "Số dư không được thay đổi khi trừ thất bại");
    }

    @Test
    @DisplayName("Trừ tiền thất bại khi userId không phải Bidder")
    void testDeductBidderBalance_NotBidder() throws AuthenticationException {
        User seller = userService.register("seller_x", "pass1234",
                "seller_x@test.com", "Seller X", UserRole.SELLER);

        boolean result = userService.deductBidderBalance(seller.getId(), 1000);

        assertFalse(result, "Trừ tiền phải thất bại khi user không phải Bidder");
    }

    @Test
    @DisplayName("Trừ tiền thất bại khi userId không tồn tại")
    void testDeductBidderBalance_NonExistentUser() {
        boolean result = userService.deductBidderBalance("non-existent-id", 1000);

        assertFalse(result, "Trừ tiền phải thất bại khi user không tồn tại");
    }

    @Test
    @DisplayName("Trừ tiền Bidder - trừ toàn bộ số dư (edge case)")
    void testDeductBidderBalance_ExactBalance() throws AuthenticationException {
        User user = userService.register("bidder3", "pass1234",
                "bidder3@test.com", "Bidder Three", UserRole.BIDDER);
        Bidder bidder = (Bidder) user;
        double initialBalance = bidder.getBalance();

        boolean result = userService.deductBidderBalance(user.getId(), initialBalance);

        assertTrue(result, "Trừ đúng bằng số dư phải thành công");
        Bidder updated = (Bidder) userService.findById(user.getId()).orElseThrow();
        assertEquals(0, updated.getBalance(), 0.01,
                "Số dư phải bằng 0 sau khi trừ hết");
    }

    // ==================== Add Seller Revenue ====================

    @Test
    @DisplayName("Cộng doanh thu Seller thành công")
    void testAddSellerRevenue_Success() throws AuthenticationException {
        User user = userService.register("seller1", "pass1234",
                "seller1@test.com", "Seller One", UserRole.SELLER);

        userService.addSellerRevenue(user.getId(), 5_000_000);

        Seller updated = (Seller) userService.findById(user.getId()).orElseThrow();
        assertEquals(5_000_000, updated.getTotalRevenue(), 0.01,
                "Doanh thu phải tăng đúng số tiền đã cộng");
    }

    @Test
    @DisplayName("Cộng doanh thu nhiều lần - tích lũy đúng")
    void testAddSellerRevenue_Accumulate() throws AuthenticationException {
        User user = userService.register("seller2", "pass1234",
                "seller2@test.com", "Seller Two", UserRole.SELLER);

        userService.addSellerRevenue(user.getId(), 1_000_000);
        userService.addSellerRevenue(user.getId(), 2_500_000);
        userService.addSellerRevenue(user.getId(), 500_000);

        Seller updated = (Seller) userService.findById(user.getId()).orElseThrow();
        assertEquals(4_000_000, updated.getTotalRevenue(), 0.01,
                "Doanh thu tích lũy phải bằng tổng các lần cộng");
    }

    @Test
    @DisplayName("Cộng doanh thu cho Bidder - không lỗi nhưng không có effect")
    void testAddSellerRevenue_NotSeller() throws AuthenticationException {
        User bidder = userService.register("bidder_y", "pass1234",
                "bidder_y@test.com", "Bidder Y", UserRole.BIDDER);

        // Không ném exception, chỉ bỏ qua
        assertDoesNotThrow(() -> userService.addSellerRevenue(bidder.getId(), 1000));
    }

    @Test
    @DisplayName("Cộng doanh thu cho userId không tồn tại - không lỗi")
    void testAddSellerRevenue_NonExistentUser() {
        assertDoesNotThrow(() -> userService.addSellerRevenue("non-existent-id", 1000));
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
