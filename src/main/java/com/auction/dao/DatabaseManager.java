package com.auction.dao;

import com.auction.exception.DataAccessException;
import com.auction.util.PasswordUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * DATABASEMANAGER - QUẢN LÝ KẾT NỐI SQLITE (SINGLETON)
 * ============================================================================
 *
 * <p>Lớp này dùng để:
 * <ol>
 *   <li>Cung cấp kết nối tới SQLite database</li>
 *   <li>Khởi tạo schema (tạo các bảng) lần đầu chạy</li>
 *   <li>Áp dụng <b>Singleton Pattern</b> - đảm bảo chỉ có 1 instance</li>
 * </ol>
 *
 * <p><b>Tại sao dùng SQLite mà không phải MySQL/PostgreSQL?</b>
 * <ul>
 *   <li>Nhẹ, không cần cài server riêng</li>
 *   <li>Toàn bộ DB nằm trong 1 file (data/auction.db) - dễ backup, xóa</li>
 *   <li>Phù hợp cho bài tập / demo - không cần triển khai phức tạp</li>
 * </ul>
 *
 * <p><b>Lưu ý quan trọng về connection:</b>
 * Mỗi lần gọi {@link #getConnection()} sẽ trả về một Connection MỚI. SQLite
 * dùng file lock nên kết nối ngắn (mở-dùng-đóng) là an toàn. Caller phải
 * đóng connection bằng try-with-resources.
 *
 * <p><b>PRAGMA foreign_keys = ON:</b> SQLite mặc định TẮT foreign key check.
 * Phải bật cho từng connection bằng PRAGMA này → ON DELETE CASCADE mới hoạt động.
 *
 * <p><b>final class:</b> không cho phép kế thừa - chống user tự viết subclass
 * phá vỡ Singleton.
 */
public final class DatabaseManager {

    /** Đường dẫn file DB. "data/" là thư mục con tự tạo nếu chưa có. */
    private static final String DB_FILE = "data/auction.db";

    /** JDBC URL chuẩn cho SQLite. */
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    /**
     * Instance Singleton.
     *
     * <p><b>volatile:</b> đảm bảo các thread nhìn thấy giá trị mới nhất.
     * Không có volatile → có thể xảy ra trường hợp 1 thread tạo xong instance
     * nhưng thread khác vẫn thấy null (do cache CPU).
     */
    private static volatile DatabaseManager instance;

    /**
     * Constructor PRIVATE (đặc trưng Singleton).
     * Tự động tạo thư mục data/ và khởi tạo schema khi instance đầu tiên được tạo.
     */
    private DatabaseManager() {
        ensureDataDir();  // Tạo thư mục data/ nếu chưa có
        initSchema();     // Tạo các bảng nếu chưa có
    }

    /**
     * Lấy instance Singleton - dùng Double-Checked Locking.
     */
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
     * Mở connection mới tới SQLite.
     *
     * <p>Caller PHẢI đóng connection sau khi dùng - khuyến nghị dùng
     * try-with-resources:
     * <pre>
     * try (Connection conn = dbm.getConnection()) {
     *     // dùng conn
     * } // tự động close ở đây
     * </pre>
     *
     * @return Connection mới, đã bật foreign_keys
     * @throws DataAccessException nếu không kết nối được
     */
    public Connection getConnection() {
        try {
            // Mở connection - SQLite tự tạo file nếu chưa có
            Connection conn = DriverManager.getConnection(DB_URL);
            // Bật foreign key check cho connection này
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            return conn;
        } catch (SQLException e) {
            // Bọc SQLException thành DataAccessException (unchecked) để
            // service không cần khai báo throws SQLException
            throw new DataAccessException("Không kết nối được SQLite: " + DB_URL, e);
        }
    }

    /**
     * Đảm bảo thư mục data/ tồn tại.
     * Nếu mkdirs() thất bại → in cảnh báo nhưng không ném exception
     * (để DriverManager báo lỗi nếu thực sự không tạo được file).
     */
    private void ensureDataDir() {
        File dir = new File("data");
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.println("Cảnh báo: không tạo được thư mục data/");
        }
    }

    /**
     * Khởi tạo các bảng trong database (lần đầu chạy).
     *
     * <p>Dùng "CREATE TABLE IF NOT EXISTS" → an toàn khi chạy nhiều lần.
     * Nếu bảng đã có sẵn → bỏ qua, không lỗi.
     *
     * <p><b>Cấu trúc các bảng:</b>
     * <ul>
     *   <li><b>users</b>: lưu mọi User (Bidder/Seller/Admin) dùng single-table
     *       inheritance - phân biệt qua cột {@code role}</li>
     *   <li><b>bidder_won_auctions</b>: lưu các phiên Bidder thắng (many-to-many)</li>
     *   <li><b>bidder_participating_auctions</b>: phiên Bidder đang tham gia</li>
     *   <li><b>seller_listed_items</b>: item Seller đang đăng</li>
     *   <li><b>items</b>: Electronics/Art/Vehicle (single-table inheritance qua category)</li>
     *   <li><b>auctions</b>: phiên đấu giá</li>
     *   <li><b>bid_transactions</b>: lịch sử bid</li>
     *   <li><b>auto_bid_configs</b>: cấu hình auto-bid</li>
     * </ul>
     *
     * <p><b>Single-table inheritance:</b> tất cả các loại User được lưu chung
     * 1 bảng `users` (với cột role); tương tự với items. Trade-off: nhiều cột
     * null nhưng query đơn giản, không cần JOIN.
     */
    private void initSchema() {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {

            // ===== BẢNG USERS (chứa cả Bidder/Seller/Admin) =====
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id              TEXT PRIMARY KEY,
                    username        TEXT UNIQUE NOT NULL,
                    password        TEXT NOT NULL,
                    email           TEXT UNIQUE NOT NULL,
                    full_name       TEXT,
                    role            TEXT NOT NULL,
                    active          INTEGER NOT NULL DEFAULT 1,
                    -- Các cột chỉ có ý nghĩa với Bidder
                    balance         REAL DEFAULT 0,
                    -- Các cột chỉ có ý nghĩa với Seller
                    total_revenue   REAL DEFAULT 0,
                    created_at      TEXT NOT NULL,
                    updated_at      TEXT NOT NULL
                )
                """);

            // ===== BẢNG QUAN HỆ: Bidder ↔ Auction đã thắng =====
            st.execute("""
                CREATE TABLE IF NOT EXISTS bidder_won_auctions (
                    bidder_id   TEXT NOT NULL,
                    auction_id  TEXT NOT NULL,
                    PRIMARY KEY (bidder_id, auction_id),
                    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);

            // ===== BẢNG QUAN HỆ: Bidder ↔ Auction đang tham gia =====
            st.execute("""
                CREATE TABLE IF NOT EXISTS bidder_participating_auctions (
                    bidder_id   TEXT NOT NULL,
                    auction_id  TEXT NOT NULL,
                    PRIMARY KEY (bidder_id, auction_id),
                    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);

            // ===== BẢNG QUAN HỆ: Seller ↔ Item đã đăng =====
            st.execute("""
                CREATE TABLE IF NOT EXISTS seller_listed_items (
                    seller_id   TEXT NOT NULL,
                    item_id     TEXT NOT NULL,
                    PRIMARY KEY (seller_id, item_id),
                    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);

            // ===== BẢNG ITEMS (chứa cả Electronics/Art/Vehicle) =====
            // Single-table inheritance: phân biệt qua cột `category`
            st.execute("""
                CREATE TABLE IF NOT EXISTS items (
                    id              TEXT PRIMARY KEY,
                    category        TEXT NOT NULL,
                    name            TEXT NOT NULL,
                    description     TEXT,
                    starting_price  REAL NOT NULL,
                    seller_id       TEXT NOT NULL,
                    image_url       TEXT,
                    -- Cột riêng cho Electronics
                    brand           TEXT,
                    model           TEXT,
                    condition_      TEXT,
                    -- Cột riêng cho Art
                    artist          TEXT,
                    art_year        INTEGER,
                    medium          TEXT,
                    -- Cột riêng cho Vehicle
                    make            TEXT,
                    vehicle_model   TEXT,
                    vehicle_year    INTEGER,
                    mileage         INTEGER,
                    created_at      TEXT NOT NULL,
                    updated_at      TEXT NOT NULL,
                    FOREIGN KEY (seller_id) REFERENCES users(id)
                )
                """);

            // ===== BẢNG AUCTIONS - phiên đấu giá =====
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

            // ===== BẢNG BID_TRANSACTIONS - lịch sử các lượt bid =====
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
            // Index để query lịch sử bid theo auction nhanh hơn
            st.execute("CREATE INDEX IF NOT EXISTS idx_bidtx_auction ON bid_transactions(auction_id, bid_time)");

            // ===== BẢNG AUTO_BID_CONFIGS - cấu hình auto-bid =====
            // PRIMARY KEY (auction_id, bidder_id) → mỗi bidder chỉ 1 config / auction
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

            seedAdmin(conn);

        } catch (SQLException e) {
            throw new DataAccessException("Khởi tạo schema SQLite thất bại", e);
        }
    }

    private void seedAdmin(Connection conn) throws SQLException {
        String check = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
        try (Statement st = conn.createStatement();
             var rs = st.executeQuery(check)) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }

        String now = LocalDateTime.now().toString();
        String sql = """
            INSERT INTO users (id, username, password, email, full_name, role, active, balance, total_revenue, created_at, updated_at)
            VALUES (?, 'admin', ?, 'admin@auction.local', 'Administrator', 'ADMIN', 1, 0, 0, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, java.util.UUID.randomUUID().toString());
            ps.setString(2, PasswordUtils.hash("1234"));
            ps.setString(3, now);
            ps.setString(4, now);
            ps.executeUpdate();
        }
    }
}
