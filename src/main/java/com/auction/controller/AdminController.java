package com.auction.controller;

import com.auction.MainApp;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.user.User;
import com.auction.network.client.AuctionClientService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;

/**
 * ============================================================================
 * ADMINCONTROLLER - CONTROLLER PANEL QUẢN TRỊ
 * ============================================================================
 *
 * <p>Màn hình admin hiển thị:
 * <ul>
 *   <li>Thống kê tổng quát: tổng số user, tổng auction, auction đang chạy</li>
 *   <li>Bảng danh sách tất cả user kèm nút "Khóa tài khoản"</li>
 * </ul>
 *
 * <p><b>Chỉ Admin mới truy cập được màn hình này</b> - kiểm tra ở
 * DashboardController trước khi navigate đến đây.
 */
public class AdminController {

    // ===== UI: Các label thống kê =====
    @FXML private Label totalUsersLabel;
    @FXML private Label totalAuctionsLabel;
    @FXML private Label activeAuctionsLabel;

    // ===== UI: Bảng danh sách user =====
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colAction; // cột chứa nút Khóa

    private final AuctionClientService auctionClientService = AuctionClientService.getInstance();

    /**
     * Initialize - chạy sau khi FXML load.
     * 1. Setup các cột bảng (cellValueFactory để gán field)
     * 2. Load dữ liệu lần đầu
     */
    @FXML
    private void initialize() {
        setupTable();
        loadData();
    }

    /**
     * Cấu hình các cột của bảng users.
     *
     * <p><b>cellValueFactory:</b> hàm trả về SimpleStringProperty cho mỗi row.
     * JavaFX dùng property để hỗ trợ binding 2 chiều (nếu data đổi → UI tự update).
     */
    private void setupTable() {
        // Mỗi cột lấy data từ field tương ứng của User
        colUsername.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUsername()));
        colFullName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFullName()));
        colEmail.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmail()));
        colRole.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRole().name()));
        // Cột status: hiển thị "Hoạt động" hoặc "Đã khóa"
        colStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isActive() ? "Hoạt động" : "Đã khóa"));

        // ===== Cột Action - custom cell có nút "Khóa tài khoản" =====
        // CellFactory tạo TableCell custom thay vì hiển thị text đơn thuần
        colAction.setCellFactory(col -> new TableCell<>() {
            // Nút "Khóa" - được tạo 1 lần / cell, tái sử dụng khi scroll
            private final Button btn = new Button("Khóa tài khoản");
            {
                // Initializer block - setup style + sự kiện click
                btn.getStyleClass().add("btn-danger");
                btn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10 4 10;");
                btn.setOnAction(e -> {
                    // Lấy User của row này
                    User user = getTableView().getItems().get(getIndex());
                    try {
                        // Gọi server deactivate
                        auctionClientService.deactivateUser(user.getId());
                        // Reload data để cập nhật trạng thái
                        loadData();
                    } catch (IOException ex) {
                        new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
                    }
                });
            }

            /**
             * updateItem được gọi mỗi khi cell cần render lại
             * (scroll, data change, sort...).
             */
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    // Row trống → không hiển thị nút
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    // Disable nút nếu user đã bị khóa
                    btn.setDisable(!user.isActive());
                    btn.setText(user.isActive() ? "Khóa" : "Đã khóa");
                    setGraphic(btn);
                }
            }
        });
    }

    /**
     * Tải dữ liệu từ server và hiển thị lên UI.
     * Gọi 2 API: getAllUsers() và getAllAuctions().
     */
    private void loadData() {
        try {
            // Lấy data từ server
            List<User> users = auctionClientService.getAllUsers();
            List<Auction> allAuctions = auctionClientService.getAllAuctions();

            // Đếm số auction đang chạy (stream + filter + count)
            long activeCount = allAuctions.stream()
                    .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                    .count();

            // ===== Update các label thống kê =====
            totalUsersLabel.setText(String.valueOf(users.size()));
            totalAuctionsLabel.setText(String.valueOf(allAuctions.size()));
            activeAuctionsLabel.setText(String.valueOf(activeCount));

            // ===== Gán list vào bảng =====
            // ObservableList: list "quan sát được" - JavaFX tự update UI khi list thay đổi
            ObservableList<User> userList = FXCollections.observableArrayList(users);
            usersTable.setItems(userList);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Không tải được dữ liệu: " + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    /** Nút "Làm mới" - load lại data. */
    @FXML
    private void handleRefresh() {
        loadData();
    }

    /** Quay về Dashboard. */
    @FXML
    private void handleBack() {
        MainApp.navigateTo("/com/auction/view/dashboard.fxml", "Trang chủ");
    }
}
