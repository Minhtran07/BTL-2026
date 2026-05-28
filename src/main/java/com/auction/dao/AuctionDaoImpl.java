package com.auction.dao;

import com.auction.exception.DataAccessException;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.auction.AutoBidConfig;
import com.auction.model.transaction.BidTransaction;

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
 * AUCTIONDAOIMPL - TRIỂN KHAI DAO CHO AUCTION (PHIÊN ĐẤU GIÁ)
 * ============================================================================
 *
 * <p>Class này phức tạp hơn UserDaoImpl/ItemDaoImpl vì Auction có 2 collection
 * con phải lưu sang bảng phụ:
 * <ol>
 *   <li>{@code bid_transactions} - lịch sử bid (1 auction → N bids)</li>
 *   <li>{@code auto_bid_configs} - cấu hình auto-bid (1 auction → N configs)</li>
 * </ol>
 *
 * <p><b>TRANSACTION HANDLING:</b> Mỗi {@code save()} chạy trong 1 transaction
 * (setAutoCommit(false) + commit/rollback) để đảm bảo nhất quán: hoặc cả
 * auction + bidHistory + autoBids đều lưu, hoặc không lưu gì cả.
 *
 * <p><b>VẤN ĐỀ FIELD PRIVATE/FINAL TRONG AUCTION:</b>
 * Auction có nhiều field {@code private} không có public setter (currentHighestBid,
 * totalBids...). Khi load từ DB, ta phải dùng reflection để gán giá trị → xem
 * {@link #setAuctionField(Auction, String, Object)}.
 *
 * <p><b>SQLite FILE LOCK:</b> SQLite tự nhất quán giữa nhiều JVM thông qua
 * file lock - không cần custom merge như phiên file-serialization cũ.
 */
public class AuctionDaoImpl implements GenericDao<Auction> {

    private final DatabaseManager db;

    public AuctionDaoImpl() {
        this.db = DatabaseManager.getInstance();
    }

    // ========================================================================
    // CRUD OPERATIONS
    // ========================================================================

    @Override
    public void save(Auction a) {
        upsertWithChildren(a);
    }

    @Override
    public void update(Auction a) {
        upsertWithChildren(a);
    }

    /**
     * Lưu Auction kèm các collection con (bidHistory, autoBids) - tất cả trong 1 transaction.
     *
     * <p><b>Cấu trúc transaction:</b>
     * <ol>
     *   <li>Bắt đầu: setAutoCommit(false)</li>
     *   <li>UPSERT bảng auctions</li>
     *   <li>DELETE + INSERT bảng bid_transactions</li>
     *   <li>DELETE + INSERT bảng auto_bid_configs</li>
     *   <li>Nếu tất cả OK → commit; nếu có lỗi → rollback</li>
     * </ol>
     */
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
            // Bắt đầu transaction
            conn.setAutoCommit(false);
            try {
                // ===== Bước 1: UPSERT bảng auctions =====
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, a.getId());
                    ps.setString(2, a.getItemId());
                    ps.setString(3, a.getSellerId());
                    ps.setString(4, a.getItemName());
                    ps.setDouble(5, a.getStartingPrice());
                    ps.setDouble(6, a.getCurrentHighestBid());
                    ps.setString(7, a.getCurrentHighestBidderId());
                    ps.setString(8, a.getCurrentHighestBidderName());
                    // LocalDateTime có thể null → check trước khi toString()
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

                // ===== Bước 2: Replace bidHistory =====
                replaceBidHistoryRows(conn, a);
                // ===== Bước 3: Replace autoBids =====
                replaceAutoBidRows(conn, a);

                // Tất cả OK → commit
                conn.commit();
            } catch (SQLException ex) {
                // Có lỗi → rollback toàn bộ
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi save auction " + a.getId(), e);
        }
    }

    /** Tìm Auction theo id - kèm load các collection con. */
    @Override
    public Optional<Auction> findById(String id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Auction a = mapRow(rs);
                loadChildren(conn, a); // load bidHistory + autoBids
                return Optional.of(a);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi findById auction " + id, e);
        }
    }

    /** Lấy tất cả Auction - kèm load collection cho mỗi auction. */
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

    /** Xóa Auction. ON DELETE CASCADE sẽ tự xóa bảng phụ. */
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
     * No-op - chỉ để tương thích với API cũ (file-serialization phiên trước).
     * SQLite read luôn nhìn thấy commit mới nhất nhờ file lock, nên không cần
     * reload thủ công.
     */
    public void reloadFromFile() {
        // no-op
    }

    // ========================================================================
    // CHILD TABLES - bid_transactions và auto_bid_configs
    // ========================================================================

    /**
     * Replace bidHistory: xóa hết → insert lại (batch).
     * Cùng trong transaction với UPSERT auctions → atomicity được đảm bảo.
     */
    private void replaceBidHistoryRows(Connection conn, Auction a) throws SQLException {
        // Xóa tất cả bid cũ của auction này
        try (PreparedStatement del = conn.prepareStatement(
                "DELETE FROM bid_transactions WHERE auction_id = ?")) {
            del.setString(1, a.getId());
            del.executeUpdate();
        }
        // Insert lại từ bidHistory hiện tại
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
                ps.addBatch(); // batch để tối ưu performance
            }
            ps.executeBatch();
        }
    }

    /** Replace auto_bid_configs - tương tự bidHistory. */
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

    /**
     * Load các bảng con (bidHistory + autoBids) vào Auction object.
     *
     * <p>bidHistory được sắp theo (bid_time, id) để giữ thứ tự gốc.
     * Sau khi load, gọi recomputeLeaderFromHistory() để đảm bảo
     * currentHighestBid khớp với bid cao nhất trong history.
     */
    private void loadChildren(Connection conn, Auction a) throws SQLException {
        // ===== Load bid history =====
        List<BidTransaction> txs = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_time, id")) {
            ps.setString(1, a.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Tạo BidTransaction với bidTime tường minh từ DB
                    BidTransaction tx = new BidTransaction(
                            rs.getString("auction_id"),
                            rs.getString("bidder_id"),
                            rs.getString("bidder_name"),
                            rs.getDouble("bid_amount"),
                            rs.getDouble("previous_bid"),
                            LocalDateTime.parse(rs.getString("bid_time")));
                    // Override id, createdAt từ DB (reflection)
                    UserDaoImpl.setEntityId(tx, rs.getString("id"));
                    UserDaoImpl.setEntityCreatedAt(tx, LocalDateTime.parse(rs.getString("created_at")));
                    txs.add(tx);
                }
            }
        }
        // Gán list bid mới vào auction
        a.replaceBidHistory(txs);
        // Tính lại leader từ history (đề phòng inconsistency)
        a.recomputeLeaderFromHistory();

        // ===== Load auto-bid configs =====
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
                    // Override registeredAt (final field - phải dùng reflection)
                    setAutoBidRegisteredAt(c, LocalDateTime.parse(rs.getString("registered_at")));
                    autos.add(c);
                }
            }
        }
        a.replaceAutoBids(autos);
    }

    /**
     * Helper: gán field final {@code registeredAt} của AutoBidConfig qua reflection.
     * AutoBidConfig là immutable nên không có setter - cần hack thông qua reflection.
     */
    private static void setAutoBidRegisteredAt(AutoBidConfig c, LocalDateTime ts) {
        try {
            Field f = AutoBidConfig.class.getDeclaredField("registeredAt");
            f.setAccessible(true);
            f.set(c, ts);
        } catch (Exception e) {
            throw new DataAccessException("Reflection set registeredAt thất bại", e);
        }
    }

    // ========================================================================
    // MAPPING
    // ========================================================================

    /**
     * Convert row → Auction object.
     *
     * <p>Phải dùng reflection vì Auction có nhiều field private không có setter
     * (currentHighestBid, totalBids, snipeExtensionCount...).
     */
    private Auction mapRow(ResultSet rs) throws SQLException {
        // Tạo Auction với các tham số constructor
        Auction a = new Auction(
                rs.getString("item_id"),
                rs.getString("seller_id"),
                rs.getString("item_name"),
                rs.getDouble("starting_price"),
                rs.getString("start_time") == null ? null : LocalDateTime.parse(rs.getString("start_time")),
                rs.getString("end_time") == null ? null : LocalDateTime.parse(rs.getString("end_time")));
        a.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        a.setAntiSnipingEnabled(rs.getInt("anti_sniping_enabled") == 1);

        // Override id, createdAt + các field private không có setter
        UserDaoImpl.setEntityId(a, rs.getString("id"));
        UserDaoImpl.setEntityCreatedAt(a, LocalDateTime.parse(rs.getString("created_at")));
        setAuctionField(a, "currentHighestBid", rs.getDouble("current_highest_bid"));
        setAuctionField(a, "currentHighestBidderId", rs.getString("current_highest_bidder_id"));
        setAuctionField(a, "currentHighestBidderName", rs.getString("current_highest_bidder_name"));
        setAuctionField(a, "totalBids", rs.getInt("total_bids"));
        setAuctionField(a, "snipeExtensionCount", rs.getInt("snipe_extension_count"));
        return a;
    }

    /** Helper reflection: set field private của Auction. */
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
