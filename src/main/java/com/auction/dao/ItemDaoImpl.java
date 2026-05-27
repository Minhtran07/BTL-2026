package com.auction.dao;

import com.auction.exception.DataAccessException;
import com.auction.model.item.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * ITEMDAOIMPL - TRIỂN KHAI DAO CHO ITEM (SINGLE-TABLE INHERITANCE)
 * ============================================================================
 *
 * <p>Quản lý CRUD cho 3 loại Item: Electronics, Art, Vehicle - tất cả lưu chung
 * 1 bảng {@code items}, phân biệt qua cột {@code category}.
 *
 * <p><b>STRUCTURE BẢNG items:</b>
 * <ul>
 *   <li>Cột chung: id, category, name, description, starting_price, seller_id, image_url</li>
 *   <li>Cột riêng Electronics: brand, model, condition_</li>
 *   <li>Cột riêng Art: artist, art_year, medium</li>
 *   <li>Cột riêng Vehicle: make, vehicle_model, vehicle_year, mileage</li>
 * </ul>
 * Mỗi row chỉ điền các cột của subtype tương ứng, các cột khác = NULL.
 */
public class ItemDaoImpl implements GenericDao<Item> {

    private final DatabaseManager db;

    public ItemDaoImpl() {
        this.db = DatabaseManager.getInstance();
    }

    /**
     * Save Item - dùng UPSERT (INSERT ... ON CONFLICT UPDATE).
     * Phải điền đúng các cột tương ứng với subtype, các cột khác set NULL.
     */
    @Override
    public void save(Item item) {
        // SQL có 19 cột "?" tương ứng với:
        // 1-7: cột chung; 8-10: Electronics; 11-13: Art; 14-17: Vehicle; 18-19: timestamps
        String sql = """
            INSERT INTO items
            (id, category, name, description, starting_price, seller_id, image_url,
             brand, model, condition_,
             artist, art_year, medium,
             make, vehicle_model, vehicle_year, mileage,
             created_at, updated_at)
            VALUES (?,?,?,?,?,?,?, ?,?,?, ?,?,?, ?,?,?,?, ?,?)
            ON CONFLICT(id) DO UPDATE SET
                category       = excluded.category,
                name           = excluded.name,
                description    = excluded.description,
                starting_price = excluded.starting_price,
                seller_id      = excluded.seller_id,
                image_url      = excluded.image_url,
                brand          = excluded.brand,
                model          = excluded.model,
                condition_     = excluded.condition_,
                artist         = excluded.artist,
                art_year       = excluded.art_year,
                medium         = excluded.medium,
                make           = excluded.make,
                vehicle_model  = excluded.vehicle_model,
                vehicle_year   = excluded.vehicle_year,
                mileage        = excluded.mileage,
                updated_at     = excluded.updated_at
            """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // ===== Set 7 cột chung =====
            ps.setString(1, item.getId());
            ps.setString(2, item.getCategory().name()); // enum → String
            ps.setString(3, item.getName());
            ps.setString(4, item.getDescription());
            ps.setDouble(5, item.getStartingPrice());
            ps.setString(6, item.getSellerId());
            ps.setString(7, item.getImageUrl());

            // ===== Set các cột riêng theo subtype =====
            // Pattern matching for instanceof (Java 16+)
            if (item instanceof Electronics e) {
                // Cột Electronics (8-10): có giá trị
                ps.setString(8, e.getBrand());
                ps.setString(9, e.getModel());
                ps.setString(10, e.getCondition());
                // Cột Art (11-13) và Vehicle (14-17): NULL
                ps.setNull(11, Types.VARCHAR);
                ps.setNull(12, Types.INTEGER);
                ps.setNull(13, Types.VARCHAR);
                ps.setNull(14, Types.VARCHAR);
                ps.setNull(15, Types.VARCHAR);
                ps.setNull(16, Types.INTEGER);
                ps.setNull(17, Types.INTEGER);
            } else if (item instanceof Art a) {
                // Cột Electronics NULL
                ps.setNull(8, Types.VARCHAR);
                ps.setNull(9, Types.VARCHAR);
                ps.setNull(10, Types.VARCHAR);
                // Cột Art có giá trị
                ps.setString(11, a.getArtist());
                ps.setInt(12, a.getYear());
                ps.setString(13, a.getMedium());
                // Cột Vehicle NULL
                ps.setNull(14, Types.VARCHAR);
                ps.setNull(15, Types.VARCHAR);
                ps.setNull(16, Types.INTEGER);
                ps.setNull(17, Types.INTEGER);
            } else if (item instanceof Vehicle v) {
                // Cột Electronics + Art NULL
                ps.setNull(8, Types.VARCHAR);
                ps.setNull(9, Types.VARCHAR);
                ps.setNull(10, Types.VARCHAR);
                ps.setNull(11, Types.VARCHAR);
                ps.setNull(12, Types.INTEGER);
                ps.setNull(13, Types.VARCHAR);
                // Cột Vehicle có giá trị
                ps.setString(14, v.getMake());
                ps.setString(15, v.getVehicleModel());
                ps.setInt(16, v.getYear());
                ps.setInt(17, v.getMileage());
            } else {
                // Loại Item chưa hỗ trợ → throw exception
                throw new DataAccessException("Item subtype không hỗ trợ: " + item.getClass());
            }

            // ===== Timestamps =====
            ps.setString(18, item.getCreatedAt().toString());
            ps.setString(19, item.getUpdatedAt().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi save item " + item.getId(), e);
        }
    }

    /** Tìm Item theo id. */
    @Override
    public Optional<Item> findById(String id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi findById item " + id, e);
        }
    }

    /** Lấy tất cả Item. */
    @Override
    public List<Item> findAll() {
        List<Item> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM items");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs));
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi findAll items", e);
        }
    }

    /** Update = save (UPSERT đã handle). */
    @Override
    public void update(Item item) {
        save(item);
    }

    /** Xóa Item theo id. */
    @Override
    public void delete(String id) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM items WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi delete item " + id, e);
        }
    }

    /**
     * Tìm tất cả Item của 1 Seller.
     *
     * <p><b>Lưu ý hiệu năng:</b> đang dùng findAll().stream().filter() →
     * load tất cả Item về rồi mới filter ở Java. Nếu dữ liệu lớn nên thay
     * bằng SQL "WHERE seller_id = ?" để filter ở DB.
     */
    public List<Item> findBySellerId(String sellerId) {
        return findAll().stream()
                .filter(i -> sellerId.equals(i.getSellerId()))
                .collect(Collectors.toList());
    }

    // ========================================================================
    // MAPPING - Convert row → Item object
    // ========================================================================

    /**
     * Chuyển 1 row → Item subclass tương ứng.
     *
     * <p>Đọc cột category → tạo Electronics/Art/Vehicle phù hợp với các field
     * của từng subtype. Sau đó dùng reflection để override id, createdAt.
     */
    private Item mapRow(ResultSet rs) throws SQLException {
        ItemCategory cat = ItemCategory.valueOf(rs.getString("category"));
        String name = rs.getString("name");
        String desc = rs.getString("description");
        double startPrice = rs.getDouble("starting_price");
        String sellerId = rs.getString("seller_id");

        Item item;
        // Tạo đúng subtype theo category
        switch (cat) {
            case ELECTRONICS -> item = new Electronics(
                    name, desc, startPrice, sellerId,
                    rs.getString("brand"), rs.getString("model"), rs.getString("condition_"));
            case ART -> item = new Art(
                    name, desc, startPrice, sellerId,
                    rs.getString("artist"), rs.getInt("art_year"), rs.getString("medium"));
            case VEHICLE -> item = new Vehicle(
                    name, desc, startPrice, sellerId,
                    rs.getString("make"), rs.getString("vehicle_model"),
                    rs.getInt("vehicle_year"), rs.getInt("mileage"));
            default -> throw new DataAccessException("Category không hỗ trợ: " + cat);
        }
        item.setImageUrl(rs.getString("image_url"));

        // Override id & createdAt từ DB (reflection - xem UserDaoImpl)
        UserDaoImpl.setEntityId(item, rs.getString("id"));
        UserDaoImpl.setEntityCreatedAt(item, LocalDateTime.parse(rs.getString("created_at")));
        return item;
    }
}
