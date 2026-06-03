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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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

    // ==================== Concurrent - Deduct Bidder Balance ====================

    @Test
    @DisplayName("Concurrent: 10 luồng đồng thời trừ tiền Bidder - không race condition")
    void testDeductBidderBalance_Concurrent() throws Exception {
        User user = userService.register("bidder_cc", "pass1234",
                "bidder_cc@test.com", "Bidder CC", UserRole.BIDDER);
        // Balance mặc định 1 tỷ, mỗi luồng trừ 100 triệu → tối đa 10 lần thành công
        double deductAmount = 100_000_000;
        int threadCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);  // Đảm bảo tất cả chạy đồng thời
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Chờ tín hiệu bắt đầu
                    if (userService.deductBidderBalance(user.getId(), deductAmount)) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();   // Bắn tín hiệu → tất cả chạy cùng lúc
        doneLatch.await();        // Chờ tất cả xong
        executor.shutdown();

        Bidder updated = (Bidder) userService.findById(user.getId()).orElseThrow();
        // Balance = 1 tỷ - (successCount * 100 triệu), phải >= 0
        assertEquals(1_000_000_000.0 - successCount.get() * deductAmount,
                updated.getBalance(), 0.01,
                "Balance phải khớp: ban đầu - (số lần thành công × số tiền trừ)");
        assertTrue(updated.getBalance() >= 0,
                "Balance không được âm dù có nhiều luồng trừ đồng thời");
        assertEquals(10, successCount.get(),
                "Đúng 10 lần trừ 100tr từ 1 tỷ phải đều thành công");
    }

    @Test
    @DisplayName("Concurrent: 20 luồng tranh nhau trừ tiền - chỉ đủ cho 10 lần")
    void testDeductBidderBalance_Concurrent_Oversaturated() throws Exception {
        User user = userService.register("bidder_ov", "pass1234",
                "bidder_ov@test.com", "Bidder OV", UserRole.BIDDER);
        // Balance 1 tỷ, mỗi lần trừ 100 triệu → chỉ đủ cho 10, 10 còn lại phải fail
        double deductAmount = 100_000_000;
        int threadCount = 20;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (userService.deductBidderBalance(user.getId(), deductAmount)) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        Bidder updated = (Bidder) userService.findById(user.getId()).orElseThrow();
        assertEquals(0, updated.getBalance(), 0.01,
                "Balance phải bằng 0 sau khi trừ hết");
        assertEquals(10, successCount.get(),
                "Chỉ đúng 10 lần trừ thành công từ 1 tỷ (mỗi lần 100tr)");
        assertEquals(10, failCount.get(),
                "10 lần còn lại phải thất bại do hết tiền");
    }

    // ==================== Concurrent - Add Seller Revenue ====================

    @Test
    @DisplayName("Concurrent: 10 luồng đồng thời cộng doanh thu Seller - không mất tiền")
    void testAddSellerRevenue_Concurrent() throws Exception {
        User user = userService.register("seller_cc", "pass1234",
                "seller_cc@test.com", "Seller CC", UserRole.SELLER);
        double addAmount = 500_000;
        int threadCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    userService.addSellerRevenue(user.getId(), addAmount);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        Seller updated = (Seller) userService.findById(user.getId()).orElseThrow();
        assertEquals(threadCount * addAmount, updated.getTotalRevenue(), 0.01,
                "Doanh thu phải bằng tổng tất cả các lần cộng, không được mất tiền");
    }

    @Test
    @DisplayName("Concurrent: cộng trừ đồng thời trên Bidder và Seller khác nhau - không conflict")
    void testConcurrent_DeductAndAdd_DifferentUsers() throws Exception {
        User bidder = userService.register("bidder_mix", "pass1234",
                "bidder_mix@test.com", "Bidder Mix", UserRole.BIDDER);
        User seller = userService.register("seller_mix", "pass1234",
                "seller_mix@test.com", "Seller Mix", UserRole.SELLER);
        int threadCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount * 2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount * 2);
        AtomicInteger deductSuccess = new AtomicInteger(0);

        // 10 luồng trừ tiền bidder (mỗi lần 50 triệu)
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (userService.deductBidderBalance(bidder.getId(), 50_000_000)) {
                        deductSuccess.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 10 luồng cộng doanh thu seller (mỗi lần 50 triệu)
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    userService.addSellerRevenue(seller.getId(), 50_000_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        Bidder updatedBidder = (Bidder) userService.findById(bidder.getId()).orElseThrow();
        Seller updatedSeller = (Seller) userService.findById(seller.getId()).orElseThrow();

        assertEquals(1_000_000_000.0 - deductSuccess.get() * 50_000_000,
                updatedBidder.getBalance(), 0.01,
                "Balance bidder phải khớp số lần trừ thành công");
        assertEquals(threadCount * 50_000_000.0, updatedSeller.getTotalRevenue(), 0.01,
                "Doanh thu seller phải đầy đủ — 2 user khác nhau không block lẫn nhau");
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
