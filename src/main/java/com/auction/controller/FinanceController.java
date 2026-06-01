package com.auction.controller;

import com.auction.MainApp;
import com.auction.SessionManager;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.transaction.BidTransaction;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.network.client.AuctionClientService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ============================================================================
 * FINANCECONTROLLER - CONTROLLER MÀN HÌNH TÀI CHÍNH
 * ============================================================================
 *
 * <p>Hiển thị thông tin tài chính của user đang đăng nhập để kiểm tra
 * tiền cộng/trừ có đúng không sau mỗi phiên đấu giá.
 *
 * <p><b>Cho Bidder:</b>
 * <ul>
 *   <li>Số dư hiện tại (balance)</li>
 *   <li>Số phiên đã thắng / đang tham gia</li>
 *   <li>Lịch sử giao dịch: mỗi bid đã đặt + settlement (trừ tiền khi thắng)</li>
 * </ul>
 *
 * <p><b>Cho Seller:</b>
 * <ul>
 *   <li>Tổng doanh thu (totalRevenue)</li>
 *   <li>Số sản phẩm đăng bán</li>
 *   <li>Lịch sử giao dịch: mỗi phiên kết thúc có người thắng → cộng doanh thu</li>
 * </ul>
 *
 * <p>Data được lấy fresh từ server mỗi lần mở hoặc bấm "Làm mới".
 */
public class FinanceController {

    // ===== UI ELEMENTS =====
    @FXML private Label userInfoLabel;
    @FXML private Label lblRole;
    @FXML private Label lblFinanceTitle;
    @FXML private Label lblFinanceValue;
    @FXML private Label lblStatsTitle;
    @FXML private Label lblStatsValue;
    @FXML private Label statusLabel;

    @FXML private TableView<TransactionRow> transactionTable;
    @FXML private TableColumn<TransactionRow, String> colTime;
    @FXML private TableColumn<TransactionRow, String> colAuction;
    @FXML private TableColumn<TransactionRow, String> colType;
    @FXML private TableColumn<TransactionRow, String> colAmount;
    @FXML private TableColumn<TransactionRow, String> colBalance;
    @FXML private TableColumn<TransactionRow, String> colNote;

    private final AuctionClientService clientService = AuctionClientService.getInstance();

    /** Formatter cho cột thời gian. */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Một dòng trong bảng lịch sử giao dịch.
     *
     * <p>Dùng record thay vì class riêng — gọn gàng, immutable,
     * auto-generate equals/hashCode/toString.
     */
    public record TransactionRow(
            String time,        // Thời gian giao dịch
            String auction,     // Tên phiên đấu giá
            String type,        // Loại: "Đặt giá" / "Thanh toán" / "Nhận tiền"
            String amount,      // Số tiền (formatted)
            String change,      // Biến động: "+500" hoặc "-2000"
            String note         // Ghi chú thêm
    ) {}

    /**
     * Initialize - load dữ liệu tài chính ngay khi mở màn hình.
     */
    @FXML
    private void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            MainApp.navigateTo("/com/auction/view/login.fxml", "Đăng nhập");
            return;
        }

        userInfoLabel.setText(user.getFullName() + " (" + user.getRole().name() + ")");

        // Setup table columns — dùng lambda để map từ TransactionRow → cell value
        colTime.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().time()));
        colAuction.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().auction()));
        colType.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().type()));
        colAmount.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().amount()));
        colBalance.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().change()));
        colNote.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().note()));

        // Tô màu cột biến động: xanh cho "+", đỏ cho "-"
        colBalance.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("+")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if (item.startsWith("-")) {
                        setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Load dữ liệu
        loadFinanceData();
    }

    /**
     * Load toàn bộ dữ liệu tài chính từ server.
     *
     * <p>Quy trình:
     * <ol>
     *   <li>Gọi getUserInfo() lấy User fresh từ DB</li>
     *   <li>Hiển thị balance/revenue lên summary cards</li>
     *   <li>Gọi getAllAuctions() + getBidHistory() để build lịch sử giao dịch</li>
     * </ol>
     */
    private void loadFinanceData() {
        statusLabel.setText("Đang tải dữ liệu...");

        try {
            // 1. Lấy user mới nhất từ server
            User freshUser = clientService.getUserInfo();

            // Cập nhật cache local
            SessionManager.getInstance().setCurrentUser(freshUser);

            // 2. Hiển thị summary theo role
            if (freshUser instanceof Bidder bidder) {
                showBidderSummary(bidder);
                loadBidderTransactions(bidder);
            } else if (freshUser instanceof Seller seller) {
                showSellerSummary(seller);
                loadSellerTransactions(seller);
            } else {
                // Admin — không có balance/revenue
                lblRole.setText("ADMIN");
                lblFinanceTitle.setText("N/A");
                lblFinanceValue.setText("—");
                lblStatsTitle.setText("N/A");
                lblStatsValue.setText("—");
            }

            statusLabel.setText("Cập nhật lúc " + java.time.LocalDateTime.now().format(TIME_FMT));

        } catch (IOException e) {
            statusLabel.setText("Lỗi: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #c0392b;");
        }
    }

    /**
     * Hiển thị summary cho Bidder: balance, số phiên thắng/tham gia.
     */
    private void showBidderSummary(Bidder bidder) {
        lblRole.setText("BIDDER (Người mua)");
        lblFinanceTitle.setText("Số dư tài khoản");
        lblFinanceValue.setText(String.format("%,.0f VNĐ", bidder.getBalance()));
        lblStatsTitle.setText("Phiên đấu giá");
        lblStatsValue.setText(String.format("Thắng: %d | Tham gia: %d",
                bidder.getWonAuctionIds().size(),
                bidder.getParticipatingAuctionIds().size()));
    }

    /**
     * Hiển thị summary cho Seller: tổng doanh thu, số item.
     */
    private void showSellerSummary(Seller seller) {
        lblRole.setText("SELLER (Người bán)");
        lblFinanceTitle.setText("Tổng doanh thu");
        lblFinanceValue.setText(String.format("%,.0f VNĐ", seller.getTotalRevenue()));
        lblStatsTitle.setText("Sản phẩm đăng bán");
        lblStatsValue.setText(seller.getListedItemIds().size() + " sản phẩm");
    }

    /**
     * Load lịch sử giao dịch cho Bidder.
     *
     * <p>Duyệt tất cả auctions, lọc ra auction mà bidder đã tham gia
     * (dựa vào bid history), hiển thị từng bid + settlement nếu đã thắng.
     */
    private void loadBidderTransactions(Bidder bidder) throws IOException {
        List<TransactionRow> rows = new ArrayList<>();
        List<Auction> allAuctions = clientService.getAllAuctions();

        for (Auction auction : allAuctions) {
            List<BidTransaction> history;
            try {
                history = clientService.getBidHistory(auction.getId());
            } catch (IOException e) {
                continue; // Bỏ qua auction lỗi
            }

            // Lọc bid của user này
            List<BidTransaction> myBids = history.stream()
                    .filter(tx -> bidder.getId().equals(tx.getBidderId()))
                    .toList();

            if (myBids.isEmpty()) continue;

            // Thêm từng bid vào bảng
            for (BidTransaction tx : myBids) {
                rows.add(new TransactionRow(
                        tx.getBidTime().format(TIME_FMT),
                        auction.getItemName(),
                        "Đặt giá",
                        String.format("%,.0f", tx.getBidAmount()),
                        "", // Đặt giá chưa trừ tiền
                        String.format("Tăng: +%,.0f", tx.getBidAmount() - tx.getPreviousBid())
                ));
            }

            // Nếu auction đã kết thúc và user là winner → thêm dòng settlement
            boolean isFinished = auction.getStatus() == AuctionStatus.FINISHED
                    || auction.getStatus() == AuctionStatus.PAID;
            if (isFinished && bidder.getId().equals(auction.getCurrentHighestBidderId())) {
                double saleAmount = auction.getCurrentHighestBid();
                rows.add(new TransactionRow(
                        auction.getUpdatedAt() != null
                                ? auction.getUpdatedAt().format(TIME_FMT)
                                : "—",
                        auction.getItemName(),
                        "Thanh toán",
                        String.format("%,.0f", saleAmount),
                        String.format("-%,.0f", saleAmount),
                        "Thắng đấu giá — trừ tiền"
                ));
            }
        }

        // Sắp xếp theo thời gian mới nhất trước
        rows.sort(Comparator.comparing(TransactionRow::time).reversed());

        ObservableList<TransactionRow> data = FXCollections.observableArrayList(rows);
        transactionTable.setItems(data);
    }

    /**
     * Load lịch sử giao dịch cho Seller.
     *
     * <p>Duyệt các auction của seller, hiển thị doanh thu nhận được
     * từ các phiên đã kết thúc thành công.
     */
    private void loadSellerTransactions(Seller seller) throws IOException {
        List<TransactionRow> rows = new ArrayList<>();
        List<Auction> allAuctions = clientService.getAllAuctions();

        for (Auction auction : allAuctions) {
            // Chỉ lấy auction của seller này
            if (!seller.getId().equals(auction.getSellerId())) continue;

            boolean isFinished = auction.getStatus() == AuctionStatus.FINISHED
                    || auction.getStatus() == AuctionStatus.PAID;
            boolean isCanceled = auction.getStatus() == AuctionStatus.CANCELED;

            if (isFinished && auction.getCurrentHighestBidderId() != null) {
                // Phiên kết thúc có người thắng → seller nhận tiền
                double saleAmount = auction.getCurrentHighestBid();
                rows.add(new TransactionRow(
                        auction.getUpdatedAt() != null
                                ? auction.getUpdatedAt().format(TIME_FMT)
                                : "—",
                        auction.getItemName(),
                        "Nhận tiền",
                        String.format("%,.0f", saleAmount),
                        String.format("+%,.0f", saleAmount),
                        "Người thắng: " + auction.getCurrentHighestBidderName()
                ));
            } else if (isFinished) {
                // Kết thúc không có ai bid
                rows.add(new TransactionRow(
                        auction.getUpdatedAt() != null
                                ? auction.getUpdatedAt().format(TIME_FMT)
                                : "—",
                        auction.getItemName(),
                        "Không bán được",
                        "0",
                        "",
                        "Không có người đặt giá"
                ));
            } else if (isCanceled) {
                rows.add(new TransactionRow(
                        auction.getUpdatedAt() != null
                                ? auction.getUpdatedAt().format(TIME_FMT)
                                : "—",
                        auction.getItemName(),
                        "Đã hủy",
                        "0",
                        "",
                        "Phiên bị hủy"
                ));
            }
            // OPEN/RUNNING → chưa có giao dịch tài chính, bỏ qua
        }

        // Sắp xếp theo thời gian mới nhất trước
        rows.sort(Comparator.comparing(TransactionRow::time).reversed());

        ObservableList<TransactionRow> data = FXCollections.observableArrayList(rows);
        transactionTable.setItems(data);
    }

    // ========================================================================
    // HANDLER CHO NÚT TRÊN TOOLBAR
    // ========================================================================

    /** Quay lại Dashboard. */
    @FXML
    private void handleBackToDashboard() {
        MainApp.navigateTo("/com/auction/view/dashboard.fxml", "Dashboard");
    }

    /** Làm mới dữ liệu tài chính (gọi lại server). */
    @FXML
    private void handleRefresh() {
        loadFinanceData();
    }
}
