package com.auction.dao;

import com.auction.exception.DataAccessException;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Quản lý kết nối SQLite (singleton).
 *
 * <p>Mỗi lần gọi {@link #getConnection()} sẽ trả về một {@link Connection}
 * mới — driver SQLite-JDBC dùng file lock cấp file system nên kết nối ngắn
 * và độc lập là an toàn. Caller có trách nhiệm đóng connection (nên dùng
 * try-with-resources).
 *
 * <p>Schema được tạo lazily ở constructor. PRAGMA {@code foreign_keys=ON}
 * cần đặt cho từng connection vì SQLite không bật mặc định.
 */
public final class DatabaseManager {

    /** File DB nằm cùng thư mục với app — dễ backup/xoá khi dev. */
    private static final String DB_FILE = "data/auction.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    private static volatile DatabaseManager instance;

    private DatabaseManager() {
        ensureDataDir();
        initSchema();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    /**
     * Mở connection mới. Caller phải đóng (try-with-resources).
     */
    public Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(DB_URL);
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            return conn;
        } catch (SQLException e) {
            throw new DataAccessException("Không kết nối được SQLite: " + DB_URL, e);
        }
    }

    private void ensureDataDir() {
        File dir = new File("data");
        if (!dir.exists() && !dir.mkdirs()) {
            // không fatal, để DriverManager báo lỗi nếu thực sự không tạo được file
            System.err.println("Cảnh báo: không tạo được thư mục data/");
        }
    }

    private void initSchema() {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            // ===== Users (single-table inheritance: BIDDER / SELLER / ADMIN) =====
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id              TEXT PRIMARY KEY,
                    username        TEXT UNIQUE NOT NULL,
                    password        TEXT NOT NULL,
                    email           TEXT UNIQUE NOT NULL,
                    full_name       TEXT,
                    role            TEXT NOT NULL,
                    active          INTEGER NOT NULL DEFAULT 1,
                    -- Bidder-only
                    balance         REAL DEFAULT 0,
                    -- Seller-only
                    total_revenue   REAL DEFAULT 0,
                    created_at      TEXT NOT NULL,
                    updated_at      TEXT NOT NULL
                )
                """);

            // Bidder.wonAuctionIds (List<String>)
            st.execute("""
                CREATE TABLE IF NOT EXISTS bidder_won_auctions (
                    bidder_id   TEXT NOT NULL,
                    auction_id  TEXT NOT NULL,
                    PRIMARY KEY (bidder_id, auction_id),
                    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);

            // Bidder.participatingAuctionIds
            st.execute("""
                CREATE TABLE IF NOT EXISTS bidder_participating_auctions (
                    bidder_id   TEXT NOT NULL,
                    auction_id  TEXT NOT NULL,
                    PRIMARY KEY (bidder_id, auction_id),
                    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);

            // Seller.listedItemIds
            st.execute("""
                CREATE TABLE IF NOT EXISTS seller_listed_items (
                    seller_id   TEXT NOT NULL,
                    item_id     TEXT NOT NULL,
                    PRIMARY KEY (seller_id, item_id),
                    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);

            // ===== Items (single-table inheritance) =====
            st.execute("""
                CREATE TABLE IF NOT EXISTS items (
                    id              TEXT PRIMARY KEY,
                    category        TEXT NOT NULL,
                    name            TEXT NOT NULL,
                    description     TEXT,
                    starting_price  REAL NOT NULL,
                    seller_id       TEXT NOT NULL,
                    image_url       TEXT,
                    -- Electronics
                    brand           TEXT,
                    model           TEXT,
                    condition_      TEXT,
                    -- Art
                    artist          TEXT,
                    art_year        INTEGER,
                    medium          TEXT,
                    -- Vehicle
                    make            TEXT,
                    vehicle_model   TEXT,
                    vehicle_year    INTEGER,
                    mileage         INTEGER,
                    created_at      TEXT NOT NULL,
                    updated_at      TEXT NOT NULL,
                    FOREIGN KEY (seller_id) REFERENCES users(id)
                )
                """);

            // ===== Auctions =====
            st.execute("""
                CREATE TABLE IF NOT EXISTS auctions (
                    id                          TEXT PRIMARY KEY,
                    item_id                     TEXT NOT NULL,
                    seller_id                   TEXT NOT NULL,
                    item_name                   TEXT,
                    starting_price              REAL NOT NULL,
                    current_highest_bid         REAL NOT NULL,
                    current_highest_bidder_id   TEXT,
                    current_highest_bidder_name TEXT,
                    start_time                  TEXT,
                    end_time                    TEXT,
                    status                      TEXT NOT NULL,
                    total_bids                  INTEGER NOT NULL DEFAULT 0,
                    anti_sniping_enabled        INTEGER NOT NULL DEFAULT 1,
                    snipe_extension_count       INTEGER NOT NULL DEFAULT 0,
                    created_at                  TEXT NOT NULL,
                    updated_at                  TEXT NOT NULL,
                    FOREIGN KEY (item_id)   REFERENCES items(id),
                    FOREIGN KEY (seller_id) REFERENCES users(id)
                )
                """);

            // Bid transactions
            st.execute("""
                CREATE TABLE IF NOT EXISTS bid_transactions (
                    id            TEXT PRIMARY KEY,
                    auction_id    TEXT NOT NULL,
                    bidder_id     TEXT NOT NULL,
                    bidder_name   TEXT,
                    bid_amount    REAL NOT NULL,
                    previous_bid  REAL NOT NULL,
                    bid_time      TEXT NOT NULL,
                    created_at    TEXT NOT NULL,
                    updated_at    TEXT NOT NULL,
                    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
                )
                """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_bidtx_auction ON bid_transactions(auction_id, bid_time)");

            // Auto-bid configs (key composite: auction_id + bidder_id)
            st.execute("""
                CREATE TABLE IF NOT EXISTS auto_bid_configs (
                    auction_id      TEXT NOT NULL,
                    bidder_id       TEXT NOT NULL,
                    bidder_name     TEXT,
                    max_bid         REAL NOT NULL,
                    increment_amt   REAL NOT NULL,
                    registered_at   TEXT NOT NULL,
                    PRIMARY KEY (auction_id, bidder_id),
                    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
                )
                """);

        } catch (SQLException e) {
            throw new DataAccessException("Khởi tạo schema SQLite thất bại", e);
        }
    }
}
