package com.auction.dao;

import com.auction.exception.DataAccessException;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * UserDao dùng SQLite (JDBC).
 *
 * <p>Áp dụng single-table inheritance: tất cả Bidder/Seller/Admin lưu chung
 * bảng {@code users}, phân biệt bằng cột {@code role}. Các collection riêng
 * của Bidder/Seller (won/participating auctions, listed items) lưu ở bảng
 * con với khoá ngoại tới {@code users.id}.
 *
 * <p>Vì các field {@code id}, {@code createdAt} của {@link com.auction.model.entity.Entity}
 * là {@code final} và được sinh ngẫu nhiên trong constructor mặc định, khi
 * load từ DB ta dùng reflection để gán đúng giá trị gốc.
 */
public class UserDaoImpl implements UserDao {

    private final DatabaseManager db;

    public UserDaoImpl() {
        this.db = DatabaseManager.getInstance();
    }

    // ---------- CRUD ----------

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
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.getId());
                ps.setString(2, user.getUsername());
                ps.setString(3, user.getPassword());
                ps.setString(4, user.getEmail());
                ps.setString(5, user.getFullName());
                ps.setString(6, user.getRole().name());
                ps.setInt(7, user.isActive() ? 1 : 0);

                if (user instanceof Bidder b) {
                    ps.setDouble(8, b.getBalance());
                    ps.setDouble(9, 0.0);
                } else if (user instanceof Seller s) {
                    ps.setDouble(8, 0.0);
                    ps.setDouble(9, s.getTotalRevenue());
                } else {
                    ps.setDouble(8, 0.0);
                    ps.setDouble(9, 0.0);
                }
                ps.setString(10, user.getCreatedAt().toString());
                ps.setString(11, user.getUpdatedAt().toString());
                ps.executeUpdate();
            }

            // Xoá hết bảng con rồi insert lại — đơn giản, idempotent
            replaceCollections(conn, user);
            conn.commit();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi save user " + user.getId(), e);
        }
    }

    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = mapRowToUser(rs);
                    loadCollections(conn, u);
                    return Optional.of(u);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi findById user " + id, e);
        }
    }

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

    @Override
    public void update(User user) {
        // INSERT...ON CONFLICT đã handle update — gọi save lại
        save(user);
    }

    @Override
    public void delete(String id) {
        // ON DELETE CASCADE sẽ xoá bảng con
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi delete user " + id, e);
        }
    }

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

    @Override
    public boolean existsByUsername(String username) {
        return countWhere("LOWER(username) = LOWER(?)", username) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        return countWhere("LOWER(email) = LOWER(?)", email) > 0;
    }

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

    // ---------- Mapping ----------

    private User mapRowToUser(ResultSet rs) throws SQLException {
        UserRole role = UserRole.valueOf(rs.getString("role"));
        String username = rs.getString("username");
        String password = rs.getString("password");
        String email = rs.getString("email");
        String fullName = rs.getString("full_name");

        User u = switch (role) {
            case BIDDER -> {
                Bidder b = new Bidder(username, password, email, fullName);
                b.setBalance(rs.getDouble("balance"));
                yield b;
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

        // Override id và createdAt bằng giá trị từ DB
        setEntityId(u, rs.getString("id"));
        setEntityCreatedAt(u, LocalDateTime.parse(rs.getString("created_at")));
        return u;
    }

    private void loadCollections(Connection conn, User u) throws SQLException {
        if (u instanceof Bidder b) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT auction_id FROM bidder_won_auctions WHERE bidder_id = ?")) {
                ps.setString(1, b.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) b.addWonAuction(rs.getString(1));
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT auction_id FROM bidder_participating_auctions WHERE bidder_id = ?")) {
                ps.setString(1, b.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) b.addParticipatingAuction(rs.getString(1));
                }
            }
        } else if (u instanceof Seller s) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT item_id FROM seller_listed_items WHERE seller_id = ?")) {
                ps.setString(1, s.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) s.addListedItem(rs.getString(1));
                }
            }
        }
    }

    private void replaceCollections(Connection conn, User u) throws SQLException {
        if (u instanceof Bidder b) {
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM bidder_won_auctions WHERE bidder_id = ?")) {
                del.setString(1, b.getId());
                del.executeUpdate();
            }
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO bidder_won_auctions(bidder_id, auction_id) VALUES (?, ?)")) {
                for (String aid : b.getWonAuctionIds()) {
                    ins.setString(1, b.getId());
                    ins.setString(2, aid);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
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

    // ---------- Reflection helpers (gán field final của Entity) ----------

    static void setEntityId(Object entity, String id) {
        setSuperField(entity, "id", id);
    }

    static void setEntityCreatedAt(Object entity, LocalDateTime ts) {
        setSuperField(entity, "createdAt", ts);
    }

    private static void setSuperField(Object entity, String fieldName, Object value) {
        try {
            Class<?> c = entity.getClass();
            while (c != null) {
                try {
                    Field f = c.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(entity, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                    c = c.getSuperclass();
                }
            }
            throw new NoSuchFieldException(fieldName);
        } catch (Exception e) {
            throw new DataAccessException("Reflection set field " + fieldName + " thất bại", e);
        }
    }
}
