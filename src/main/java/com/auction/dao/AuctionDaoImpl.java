package com.auction.dao;

import com.auction.exception.DataAccessException;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.auction.AutoBidConfig;
import com.auction.model.transaction.BidTransaction;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * AuctionDao dùng SQLite.
 *
 * <p>Mỗi {@link Auction} được lưu thành 3 bảng:
 * <ul>
 *   <li>{@code auctions} — thuộc tính scalar của auction</li>
 *   <li>{@code bid_transactions} — bidHistory (1-N)</li>
 *   <li>{@code auto_bid_configs} — autoBids (1-N, key bidder_id duy nhất)</li>
 * </ul>
 *
 * <p>SQLite mặc định serialize ghi bằng file lock nên không cần custom merge
 * như phiên bản file-serialization cũ. Mỗi {@code save/update} chạy trong
 * transaction để bid history và scalar fields đồng bộ.
 */
public class AuctionDaoImpl implements GenericDao<Auction> {

    private final DatabaseManager db;

    public AuctionDaoImpl() {
        this.db = DatabaseManager.getInstance();
    }

    // ---------- CRUD ----------

    @Override
    public void save(Auction a) {
        upsertWithChildren(a);
    }

    @Override
    public void update(Auction a) {
        upsertWithChildren(a);
    }

    private void upsertWithChildren(Auction a) {
        String sql = """
            INSERT INTO auctions
            (id, item_id, seller_id, item_name, starting_price,
             current_highest_bid, current_highest_bidder_id, current_highest_bidder_name,
             start_time, end_time, status, total_bids,
             anti_sniping_enabled, snipe_extension_count,
             created_at, updated_at)
            VALUES (?,?,?,?,?, ?,?,?, ?,?,?,?, ?,?, ?,?)
            ON CONFLICT(id) DO UPDATE SET
                item_id                     = excluded.item_id,
                seller_id                   = excluded.seller_id,
                item_name                   = excluded.item_name,
                starting_price              = excluded.starting_price,
                current_highest_bid         = excluded.current_highest_bid,
                current_highest_bidder_id   = excluded.current_highest_bidder_id,
                current_highest_bidder_name = excluded.current_highest_bidder_name,
                start_time                  = excluded.start_time,
                end_time                    = excluded.end_time,
                status                      = excluded.status,
                total_bids                  = excluded.total_bids,
                anti_sniping_enabled        = excluded.anti_sniping_enabled,
                snipe_extension_count       = excluded.snipe_extension_count,
                updated_at                  = excluded.updated_at
            """;

        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, a.getId());
                    ps.setString(2, a.getItemId());
                    ps.setString(3, a.getSellerId());
                    ps.setString(4, a.getItemName());
                    ps.setDouble(5, a.getStartingPrice());
                    ps.setDouble(6, a.getCurrentHighestBid());
                    ps.setString(7, a.getCurrentHighestBidderId());
                    ps.setString(8, a.getCurrentHighestBidderName());
                    ps.setString(9, a.getStartTime() == null ? null : a.getStartTime().toString());
                    ps.setString(10, a.getEndTime() == null ? null : a.getEndTime().toString());
                    ps.setString(11, a.getStatus().name());
                    ps.setInt(12, a.getTotalBids());
                    ps.setInt(13, a.isAntiSnipingEnabled() ? 1 : 0);
                    ps.setInt(14, a.getSnipeExtensionCount());
                    ps.setString(15, a.getCreatedAt().toString());
                    ps.setString(16, a.getUpdatedAt().toString());
                    ps.executeUpdate();
                }

                replaceBidHistoryRows(conn, a);
                replaceAutoBidRows(conn, a);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi save auction " + a.getId(), e);
        }
    }

    @Override
    public Optional<Auction> findById(String id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Auction a = mapRow(rs);
                loadChildren(conn, a);
                return Optional.of(a);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi findById auction " + id, e);
        }
    }

    @Override
    public List<Auction> findAll() {
        List<Auction> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM auctions");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Auction a = mapRow(rs);
                loadChildren(conn, a);
                result.add(a);
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi findAll auctions", e);
        }
    }

    @Override
    public void delete(String id) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM auctions WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi delete auction " + id, e);
        }
    }

    /**
     * Tương thích với API cũ (file-serialization). SQLite tự nhất quán giữa
     * nhiều JVM thông qua file lock + transaction; mỗi lần findById/findAll
     * đã là một fresh read nên không cần làm gì thêm.
     */
    public void reloadFromFile() {
        // no-op: SQLite read luôn nhìn thấy commit mới nhất
    }

    // ---------- Children ----------

    private void replaceBidHistoryRows(Connection conn, Auction a) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement(
                "DELETE FROM bid_transactions WHERE auction_id = ?")) {
            del.setString(1, a.getId());
            del.executeUpdate();
        }
        String ins = """
            INSERT INTO bid_transactions
            (id, auction_id, bidder_id, bidder_name, bid_amount, previous_bid,
             bid_time, created_at, updated_at)
            VALUES (?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(ins)) {
            for (BidTransaction tx : a.getBidHistory()) {
                ps.setString(1, tx.getId());
                ps.setString(2, tx.getAuctionId());
                ps.setString(3, tx.getBidderId());
                ps.setString(4, tx.getBidderName());
                ps.setDouble(5, tx.getBidAmount());
                ps.setDouble(6, tx.getPreviousBid());
                ps.setString(7, tx.getBidTime().toString());
                ps.setString(8, tx.getCreatedAt().toString());
                ps.setString(9, tx.getUpdatedAt().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void replaceAutoBidRows(Connection conn, Auction a) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement(
                "DELETE FROM auto_bid_configs WHERE auction_id = ?")) {
            del.setString(1, a.getId());
            del.executeUpdate();
        }
        String ins = """
            INSERT INTO auto_bid_configs
            (auction_id, bidder_id, bidder_name, max_bid, increment_amt, registered_at)
            VALUES (?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(ins)) {
            for (AutoBidConfig c : a.getAutoBids()) {
                ps.setString(1, a.getId());
                ps.setString(2, c.getBidderId());
                ps.setString(3, c.getBidderName());
                ps.setDouble(4, c.getMaxBid());
                ps.setDouble(5, c.getIncrement());
                ps.setString(6, c.getRegisteredAt().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void loadChildren(Connection conn, Auction a) throws SQLException {
        // Bid history
        List<BidTransaction> txs = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_time, id")) {
            ps.setString(1, a.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BidTransaction tx = new BidTransaction(
                            rs.getString("auction_id"),
                            rs.getString("bidder_id"),
                            rs.getString("bidder_name"),
                            rs.getDouble("bid_amount"),
                            rs.getDouble("previous_bid"),
                            LocalDateTime.parse(rs.getString("bid_time")));
                    UserDaoImpl.setEntityId(tx, rs.getString("id"));
                    UserDaoImpl.setEntityCreatedAt(tx, LocalDateTime.parse(rs.getString("created_at")));
                    txs.add(tx);
                }
            }
        }
        a.replaceBidHistory(txs);
        a.recomputeLeaderFromHistory();

        // Auto bids
        List<AutoBidConfig> autos = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM auto_bid_configs WHERE auction_id = ?")) {
            ps.setString(1, a.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AutoBidConfig c = new AutoBidConfig(
                            rs.getString("bidder_id"),
                            rs.getString("bidder_name"),
                            rs.getDouble("max_bid"),
                            rs.getDouble("increment_amt"));
                    // override registeredAt
                    setAutoBidRegisteredAt(c, LocalDateTime.parse(rs.getString("registered_at")));
                    autos.add(c);
                }
            }
        }
        a.replaceAutoBids(autos);
    }

    private static void setAutoBidRegisteredAt(AutoBidConfig c, LocalDateTime ts) {
        try {
            Field f = AutoBidConfig.class.getDeclaredField("registeredAt");
            f.setAccessible(true);
            f.set(c, ts);
        } catch (Exception e) {
            throw new DataAccessException("Reflection set registeredAt thất bại", e);
        }
    }

    // ---------- Mapping ----------

    private Auction mapRow(ResultSet rs) throws SQLException {
        Auction a = new Auction(
                rs.getString("item_id"),
                rs.getString("seller_id"),
                rs.getString("item_name"),
                rs.getDouble("starting_price"),
                rs.getString("start_time") == null ? null : LocalDateTime.parse(rs.getString("start_time")),
                rs.getString("end_time") == null ? null : LocalDateTime.parse(rs.getString("end_time")));
        a.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        a.setAntiSnipingEnabled(rs.getInt("anti_sniping_enabled") == 1);

        // Override id, createdAt, các field private không có setter
        UserDaoImpl.setEntityId(a, rs.getString("id"));
        UserDaoImpl.setEntityCreatedAt(a, LocalDateTime.parse(rs.getString("created_at")));
        setAuctionField(a, "currentHighestBid", rs.getDouble("current_highest_bid"));
        setAuctionField(a, "currentHighestBidderId", rs.getString("current_highest_bidder_id"));
        setAuctionField(a, "currentHighestBidderName", rs.getString("current_highest_bidder_name"));
        setAuctionField(a, "totalBids", rs.getInt("total_bids"));
        setAuctionField(a, "snipeExtensionCount", rs.getInt("snipe_extension_count"));
        return a;
    }

    private static void setAuctionField(Auction a, String field, Object value) {
        try {
            Field f = Auction.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(a, value);
        } catch (Exception e) {
            throw new DataAccessException("Reflection set " + field + " thất bại", e);
        }
    }
}
