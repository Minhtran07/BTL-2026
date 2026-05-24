package com.auction.dao;

import com.auction.exception.DataAccessException;
import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.ItemCategory;
import com.auction.model.item.Vehicle;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ItemDao dùng SQLite với single-table inheritance.
 *
 * <p>Tất cả Electronics/Art/Vehicle lưu chung bảng {@code items}, phân biệt
 * bằng cột {@code category}. Các thuộc tính riêng của từng subtype lưu vào
 * cột tương ứng (NULL với các subtype khác).
 */
public class ItemDaoImpl implements GenericDao<Item> {

    private final DatabaseManager db;

    public ItemDaoImpl() {
        this.db = DatabaseManager.getInstance();
    }

    @Override
    public void save(Item item) {
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
            ps.setString(1, item.getId());
            ps.setString(2, item.getCategory().name());
            ps.setString(3, item.getName());
            ps.setString(4, item.getDescription());
            ps.setDouble(5, item.getStartingPrice());
            ps.setString(6, item.getSellerId());
            ps.setString(7, item.getImageUrl());

            // subtype-specific columns
            if (item instanceof Electronics e) {
                ps.setString(8, e.getBrand());
                ps.setString(9, e.getModel());
                ps.setString(10, e.getCondition());
                ps.setNull(11, Types.VARCHAR);
                ps.setNull(12, Types.INTEGER);
                ps.setNull(13, Types.VARCHAR);
                ps.setNull(14, Types.VARCHAR);
                ps.setNull(15, Types.VARCHAR);
                ps.setNull(16, Types.INTEGER);
                ps.setNull(17, Types.INTEGER);
            } else if (item instanceof Art a) {
                ps.setNull(8, Types.VARCHAR);
                ps.setNull(9, Types.VARCHAR);
                ps.setNull(10, Types.VARCHAR);
                ps.setString(11, a.getArtist());
                ps.setInt(12, a.getYear());
                ps.setString(13, a.getMedium());
                ps.setNull(14, Types.VARCHAR);
                ps.setNull(15, Types.VARCHAR);
                ps.setNull(16, Types.INTEGER);
                ps.setNull(17, Types.INTEGER);
            } else if (item instanceof Vehicle v) {
                ps.setNull(8, Types.VARCHAR);
                ps.setNull(9, Types.VARCHAR);
                ps.setNull(10, Types.VARCHAR);
                ps.setNull(11, Types.VARCHAR);
                ps.setNull(12, Types.INTEGER);
                ps.setNull(13, Types.VARCHAR);
                ps.setString(14, v.getMake());
                ps.setString(15, v.getVehicleModel());
                ps.setInt(16, v.getYear());
                ps.setInt(17, v.getMileage());
            } else {
                throw new DataAccessException("Item subtype không hỗ trợ: " + item.getClass());
            }

            ps.setString(18, item.getCreatedAt().toString());
            ps.setString(19, item.getUpdatedAt().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi save item " + item.getId(), e);
        }
    }

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

    @Override
    public void update(Item item) {
        save(item);
    }

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

    public List<Item> findBySellerId(String sellerId) {
        return findAll().stream()
                .filter(i -> sellerId.equals(i.getSellerId()))
                .collect(Collectors.toList());
    }

    // ---------- Mapping ----------

    private Item mapRow(ResultSet rs) throws SQLException {
        ItemCategory cat = ItemCategory.valueOf(rs.getString("category"));
        String name = rs.getString("name");
        String desc = rs.getString("description");
        double startPrice = rs.getDouble("starting_price");
        String sellerId = rs.getString("seller_id");

        Item item;
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

        UserDaoImpl.setEntityId(item, rs.getString("id"));
        UserDaoImpl.setEntityCreatedAt(item, LocalDateTime.parse(rs.getString("created_at")));
        return item;
    }
}
