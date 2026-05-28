package com.auction.dao;

import com.auction.exception.DataAccessException;
import com.auction.model.user.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * USERDAOIMPL - TRIỂN KHAI CỤ THỂ CỦA USERDAO BẰNG SQLITE
 * ============================================================================
 *
 * <p>Class này là phần "đầu việc" thực sự của UserDao - dùng JDBC để giao tiếp
 * với SQLite database.
 *
 * <p><b>SINGLE-TABLE INHERITANCE PATTERN:</b>
 * Cả 3 loại User (Bidder/Seller/Admin) được lưu chung 1 bảng {@code users},
 * phân biệt qua cột {@code role}. Trade-off:
 * <ul>
 *   <li>Ưu: query đơn giản, không cần JOIN khi đọc User</li>
 *   <li>Nhược: nhiều cột NULL (vd: Admin không có balance/total_revenue)</li>
 * </ul>
 *
 * <p><b>COLLECTION FIELDS:</b>
 * Các danh sách của Bidder (won/participating auctions) và Seller (listed items)
 * không thể lưu trong 1 ô của bảng users → lưu sang các bảng phụ riêng:
 * <ul>
 *   <li>{@code bidder_won_auctions} - Bidder thắng phiên nào</li>
 *   <li>{@code bidder_participating_auctions} - Bidder tham gia phiên nào</li>
 *   <li>{@code seller_listed_items} - Seller đăng item nào</li>
 * </ul>
 *
 * <p><b>VẤN ĐỀ: Field final trong Entity:</b>
 * Lớp Entity có {@code id} và {@code createdAt} là {@code final} - được sinh
 * trong constructor mặc định. Khi load từ DB → không thể gán lại bằng setter.
 * <b>Giải pháp:</b> dùng Java Reflection để bypass final - xem
 * {@link #setSuperField(Object, String, Object)}.
 */
public class UserDaoImpl implements UserDao {

    /** Database manager - lấy connection từ đây. */
    private final DatabaseManager db;

    /** Constructor: lấy instance Singleton của DatabaseManager. */
    public UserDaoImpl() {
        this.db = DatabaseManager.getInstance();
    }

    // ========================================================================
    // CRUD OPERATIONS - Tạo, Đọc, Cập nhật, Xóa
    // ========================================================================

    /**
     * Lưu User vào DB.
     *
     * <p>Dùng "INSERT ... ON CONFLICT DO UPDATE" của SQLite (UPSERT):
     * <ul>
     *   <li>Nếu id chưa tồn tại → INSERT mới</li>
     *   <li>Nếu id đã tồn tại → UPDATE các field</li>
     * </ul>
     * Cách này đơn giản hơn việc kiểm tra exist rồi mới quyết định INSERT/UPDATE.
     */
    @Override
    public void save(User user) {
        String sql = """
            INSERT INTO users
            (id, username, password, email, full_name, role, active,
             balance, total_revenue, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                username      = excluded.username,
                password      = excluded.password,
                email         = excluded.email,
                full_name     = excluded.full_name,
                role          = excluded.role,
                active        = excluded.active,
                balance       = excluded.balance,
                total_revenue = excluded.total_revenue,
                updated_at    = excluded.updated_at
            """;

        try (Connection conn = db.getConnection()) {
            // Tắt auto-commit để chạy trong 1 transaction
            // → nếu insert vào bảng phụ thất bại, rollback luôn bảng users
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                // Set tham số ? theo thứ tự
                ps.setString(1, user.getId());
                ps.setString(2, user.getUsername());
                ps.setString(3, user.getPassword());
                ps.setString(4, user.getEmail());
                ps.setString(5, user.getFullName());
                ps.setString(6, user.getRole().name());
                ps.setInt(7, user.isActive() ? 1 : 0); // SQLite không có boolean → dùng 0/1

                // Set field riêng theo loại User (instanceof pattern matching - Java 16+)
                if (user instanceof Bidder b) {
                    ps.setDouble(8, b.getBalance());
                    ps.setDouble(9, 0.0); // Bidder không có total_revenue
                } else if (user instanceof Seller s) {
                    ps.setDouble(8, 0.0); // Seller không có balance
                    ps.setDouble(9, s.getTotalRevenue());
                } else {
                    // Admin → cả 2 đều 0
                    ps.setDouble(8, 0.0);
                    ps.setDouble(9, 0.0);
                }
                // LocalDateTime → String ISO format ("2026-05-22T10:30:00")
                ps.setString(10, user.getCreatedAt().toString());
                ps.setString(11, user.getUpdatedAt().toString());
                ps.executeUpdate();
            }

            // Cập nhật các bảng phụ (collections): xóa hết rồi insert lại
            replaceCollections(conn, user);

            // Commit transaction - mọi thay đổi sẽ được ghi vào file DB
            conn.commit();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi save user " + user.getId(), e);
        }
    }

    /** Tìm User theo id. Trả Optional.empty() nếu không có. */
    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Map row → User object
                    User u = mapRowToUser(rs);
                    // Load thêm các collection từ bảng phụ
                    loadCollections(conn, u);
                    return Optional.of(u);
                }
                return Optional.empty(); // không có row nào → không tìm thấy
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi findById user " + id, e);
        }
    }

    /** Lấy tất cả User trong bảng. */
    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        List<User> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = mapRowToUser(rs);
                loadCollections(conn, u);
                result.add(u);
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi findAll users", e);
        }
    }

    /**
     * Update user. Vì save() đã dùng UPSERT (INSERT ... ON CONFLICT UPDATE)
     * → chỉ cần gọi save() lại là đủ.
     */
    @Override
    public void update(User user) {
        save(user);
    }

    /**
     * Xóa user theo id.
     * ON DELETE CASCADE trong schema sẽ tự xóa các record liên quan ở bảng phụ.
     */
    @Override
    public void delete(String id) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi delete user " + id, e);
        }
    }

    /**
     * Tìm User theo username.
     * LOWER(username) = LOWER(?) → so sánh không phân biệt hoa thường.
     */
    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = mapRowToUser(rs);
                    loadCollections(conn, u);
                    return Optional.of(u);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi findByUsername " + username, e);
        }
    }

    /** Kiểm tra username đã tồn tại - dùng SELECT COUNT(*) tối ưu hơn. */
    @Override
    public boolean existsByUsername(String username) {
        return countWhere("LOWER(username) = LOWER(?)", username) > 0;
    }

    /** Kiểm tra email đã tồn tại. */
    @Override
    public boolean existsByEmail(String email) {
        return countWhere("LOWER(email) = LOWER(?)", email) > 0;
    }

    /**
     * Helper: đếm số row khớp với điều kiện whereClause.
     * Trả về int count thay vì load toàn bộ User → nhanh hơn nhiều.
     */
    private int countWhere(String whereClause, String param) {
        String sql = "SELECT COUNT(*) FROM users WHERE " + whereClause;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi count users", e);
        }
    }

    // ========================================================================
    // MAPPING - Chuyển ResultSet → User object
    // ========================================================================

    /**
     * Chuyển 1 row từ ResultSet thành User object phù hợp.
     *
     * <p><b>Logic:</b>
     * <ol>
     *   <li>Đọc cột role để biết loại User</li>
     *   <li>Tạo Bidder/Seller/Admin tương ứng (switch expression - Java 14+)</li>
     *   <li>Set thêm các field riêng (balance, total_revenue)</li>
     *   <li>Override id, createdAt bằng reflection (vì là field final)</li>
     * </ol>
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        // Đọc role từ DB (String) → convert sang enum
        UserRole role = UserRole.valueOf(rs.getString("role"));
        String username = rs.getString("username");
        String password = rs.getString("password");
        String email = rs.getString("email");
        String fullName = rs.getString("full_name");

        // Switch expression - tạo đúng loại User theo role
        User u = switch (role) {
            case BIDDER -> {
                Bidder b = new Bidder(username, password, email, fullName);
                b.setBalance(rs.getDouble("balance"));
                yield b; // trả về Bidder cho biến u
            }
            case SELLER -> {
                Seller s = new Seller(username, password, email, fullName);
                double rev = rs.getDouble("total_revenue");
                if (rev > 0) s.addRevenue(rev);
                yield s;
            }
            case ADMIN -> new Admin(username, password, email, fullName);
        };
        u.setActive(rs.getInt("active") == 1);

        // Override id và createdAt từ DB (vì constructor đã sinh giá trị random)
        setEntityId(u, rs.getString("id"));
        setEntityCreatedAt(u, LocalDateTime.parse(rs.getString("created_at")));
        return u;
    }

    /**
     * Load các collection từ bảng phụ vào User object.
     * Chỉ load nếu User là Bidder (won/participating) hoặc Seller (listed items).
     * Admin không có collection nên skip.
     */
    private void loadCollections(Connection conn, User u) throws SQLException {
        if (u instanceof Bidder b) {
            // ===== Load won auctions =====
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT auction_id FROM bidder_won_auctions WHERE bidder_id = ?")) {
                ps.setString(1, b.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) b.addWonAuction(rs.getString(1));
                }
            }
            // ===== Load participating auctions =====
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT auction_id FROM bidder_participating_auctions WHERE bidder_id = ?")) {
                ps.setString(1, b.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) b.addParticipatingAuction(rs.getString(1));
                }
            }
        } else if (u instanceof Seller s) {
            // ===== Load listed items =====
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT item_id FROM seller_listed_items WHERE seller_id = ?")) {
                ps.setString(1, s.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) s.addListedItem(rs.getString(1));
                }
            }
        }
    }

    /**
     * Cập nhật bảng phụ: xóa hết → insert lại toàn bộ.
     *
     * <p><b>Tại sao xóa hết rồi insert lại?</b> Đơn giản và idempotent
     * (chạy nhiều lần kết quả như nhau). Cách khác (diff để chỉ thêm/xóa
     * những cái thay đổi) phức tạp hơn nhiều, không đáng cho dự án này.
     *
     * <p>Dùng batch INSERT (addBatch + executeBatch) để tối ưu performance
     * khi có nhiều record.
     */
    private void replaceCollections(Connection conn, User u) throws SQLException {
        if (u instanceof Bidder b) {
            // ===== Won auctions =====
            // Xóa toàn bộ won auctions của bidder
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM bidder_won_auctions WHERE bidder_id = ?")) {
                del.setString(1, b.getId());
                del.executeUpdate();
            }
            // Insert lại danh sách mới (batch insert)
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO bidder_won_auctions(bidder_id, auction_id) VALUES (?, ?)")) {
                for (String aid : b.getWonAuctionIds()) {
                    ins.setString(1, b.getId());
                    ins.setString(2, aid);
                    ins.addBatch(); // gom vào batch, chưa execute
                }
                ins.executeBatch(); // execute tất cả 1 lần
            }
            // ===== Participating auctions =====
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM bidder_participating_auctions WHERE bidder_id = ?")) {
                del.setString(1, b.getId());
                del.executeUpdate();
            }
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO bidder_participating_auctions(bidder_id, auction_id) VALUES (?, ?)")) {
                for (String aid : b.getParticipatingAuctionIds()) {
                    ins.setString(1, b.getId());
                    ins.setString(2, aid);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
        } else if (u instanceof Seller s) {
            // ===== Listed items =====
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM seller_listed_items WHERE seller_id = ?")) {
                del.setString(1, s.getId());
                del.executeUpdate();
            }
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO seller_listed_items(seller_id, item_id) VALUES (?, ?)")) {
                for (String iid : s.getListedItemIds()) {
                    ins.setString(1, s.getId());
                    ins.setString(2, iid);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
        }
    }

    // ========================================================================
    // REFLECTION HELPERS - Hack để gán field FINAL của Entity
    // ========================================================================

    /**
     * Set field "id" của Entity thông qua reflection.
     *
     * <p><b>Tại sao cần?</b> Trong Entity, field id là {@code private final}
     * String được sinh trong constructor (UUID.randomUUID()). Khi load từ DB,
     * ta cần ghi đè bằng id thật, nhưng final field không thể gán lại bằng
     * setter thông thường. → Phải dùng reflection để bypass.
     */
    static void setEntityId(Object entity, String id) {
        setSuperField(entity, "id", id);
    }

    /** Tương tự setEntityId nhưng cho field createdAt. */
    static void setEntityCreatedAt(Object entity, LocalDateTime ts) {
        setSuperField(entity, "createdAt", ts);
    }

    /**
     * Helper chung: dùng reflection để set field private của lớp cha (Entity).
     *
     * <p><b>Cách hoạt động:</b>
     * <ol>
     *   <li>Lấy Class của entity</li>
     *   <li>Duyệt lên các lớp cha cho đến khi tìm được field</li>
     *   <li>Set accessible = true (bypass private)</li>
     *   <li>Set giá trị mới</li>
     * </ol>
     *
     * <p><b>Cảnh báo:</b> Reflection là kỹ thuật mạnh nhưng nên hạn chế dùng.
     * Trong dự án thật, có thể design khác (vd: constructor protected nhận
     * id, createdAt) để tránh reflection.
     */
    private static void setSuperField(Object entity, String fieldName, Object value) {
        try {
            Class<?> c = entity.getClass();
            // Duyệt lên cây thừa kế tìm field
            while (c != null) {
                try {
                    Field f = c.getDeclaredField(fieldName);
                    f.setAccessible(true); // bypass private
                    f.set(entity, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                    // Không có ở class này → thử lớp cha
                    c = c.getSuperclass();
                }
            }
            throw new NoSuchFieldException(fieldName);
        } catch (Exception e) {
            throw new DataAccessException("Reflection set field " + fieldName + " thất bại", e);
        }
    }
}
