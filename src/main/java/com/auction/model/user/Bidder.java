package com.auction.model.user;



import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * LỚP BIDDER - NGƯỜI THAM GIA ĐẤU GIÁ (NGƯỜI MUA)
 * ============================================================================
 *
 * <p>Bidder là một loại User cụ thể, kế thừa từ {@link User}.
 * Đây là người dùng có khả năng đặt giá (bid) cho các phiên đấu giá.
 *
 * <p><b>Đặc điểm riêng của Bidder so với User chung:</b>
 * <ul>
 *   <li>Có <b>số dư tài khoản (balance)</b> - tiền dùng để đặt giá</li>
 *   <li>Có danh sách <b>phiên đã thắng</b> (wonAuctionIds)</li>
 *   <li>Có danh sách <b>phiên đang tham gia</b> (participatingAuctionIds)</li>
 * </ul>
 *
 * <p><b>Polymorphism:</b> Override {@link #getRole()} trả về
 * {@code UserRole.BIDDER}, override {@link #printInfo()} để format riêng.
 */
public class Bidder extends User {

    private static final long serialVersionUID = 1L;

    /** Số dư tài khoản hiện có (đơn vị: VNĐ hoặc USD tùy quy ước). */
    private double balance;

    /**
     * Danh sách ID các phiên Bidder đã thắng.
     * Khai báo {@code final} → tham chiếu list không đổi, nhưng nội dung có
     * thể thay đổi (add/remove). Tránh việc gán list mới lên field.
     */
    private final List<String> wonAuctionIds;

    /** Danh sách ID các phiên Bidder đang tham gia (đã đặt giá). */
    private final List<String> participatingAuctionIds;

    /**
     * Constructor không tham số - chủ yếu cho Java Serialization.
     * Khởi tạo balance = 0 và danh sách rỗng.
     */
    public Bidder() {
        super();
        this.balance = 0.0;
        this.wonAuctionIds = new ArrayList<>();
        this.participatingAuctionIds = new ArrayList<>();
    }

    /**
     * Constructor đầy đủ - dùng khi đăng ký Bidder mới.
     * <p>Bidder mới được tặng {@code 1_000_000_000.0} làm balance khởi tạo
     * (demo mode - dự án bài tập, không có cổng thanh toán thật).
     */
    public Bidder(String username, String password, String email, String fullName) {
        super(username, password, email, fullName);
        this.balance = 1_000_000_000.0;
        this.wonAuctionIds = new ArrayList<>();
        this.participatingAuctionIds = new ArrayList<>();
    }

    /**
     * Override getRole() → trả về BIDDER. Polymorphism tại runtime.
     */
    @Override
    public UserRole getRole() {
        return UserRole.BIDDER;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
        markUpdated();
    }

    /**
     * Cộng thêm tiền vào balance (ví dụ: nạp tiền, hoàn tiền sau khi thua đấu giá).
     *
     * @param amount số tiền cần cộng (giả định không âm)
     */
    public void addBalance(double amount) {
        this.balance += amount;
        markUpdated();
    }

    /**
     * Trừ tiền khỏi balance.
     *
     * <p>Kiểm tra số dư trước khi trừ - nếu không đủ thì trả về false và
     * KHÔNG trừ. Đây là cơ chế chống âm balance đơn giản.
     *
     * @param amount số tiền cần trừ
     * @return {@code true} nếu trừ thành công, {@code false} nếu không đủ tiền
     */
    public boolean deductBalance(double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            markUpdated();
            return true;
        }
        return false; // Không đủ số dư
    }

    /**
     * Trả về một BẢN SAO của danh sách phiên đã thắng.
     *
     * <p><b>Lý do trả về bản sao thay vì list gốc:</b> nếu trả gốc, code
     * bên ngoài có thể {@code list.clear()} → phá vỡ encapsulation. Trả
     * bản sao đảm bảo nội bộ class kiểm soát hoàn toàn list.
     */
    public List<String> getWonAuctionIds() {
        return new ArrayList<>(wonAuctionIds); // defensive copy
    }

    /** Thêm 1 phiên vào danh sách đã thắng (gọi khi kết thúc auction). */
    public void addWonAuction(String auctionId) {
        wonAuctionIds.add(auctionId);
        markUpdated();
    }

    public List<String> getParticipatingAuctionIds() {
        return new ArrayList<>(participatingAuctionIds);
    }

    /**
     * Thêm vào danh sách phiên đang tham gia.
     * Kiểm tra trùng lặp - nếu đã có rồi thì bỏ qua (không add lần nữa).
     */
    public void addParticipatingAuction(String auctionId) {
        if (!participatingAuctionIds.contains(auctionId)) {
            participatingAuctionIds.add(auctionId);
            markUpdated();
        }
    }

    @Override
    public String printInfo() {
        return String.format("Bidder[id=%s, username=%s, balance=%.2f, wonAuctions=%d]",
                getId(), getUsername(), balance, wonAuctionIds.size());
    }
}
