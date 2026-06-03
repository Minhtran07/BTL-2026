package com.auction.controller;

import com.auction.MainApp;
import com.auction.SessionManager;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.client.AuctionClient;
import com.auction.network.client.AuctionClientService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * DASHBOARDCONTROLLER - CONTROLLER MÀN HÌNH CHÍNH (TRANG DASHBOARD)
 * ============================================================================
 *
 * <p>Đây là màn hình "trung tâm" sau khi đăng nhập. Hiển thị:
 * <ul>
 *   <li>Danh sách các phiên đấu giá dưới dạng các "card"</li>
 *   <li>Filter theo trạng thái + search theo tên</li>
 *   <li>Toolbar action: tạo phiên (Seller), Admin panel (Admin), logout</li>
 *   <li>Realtime update khi có bid mới (push từ server)</li>
 *   <li>Auto-refresh countdown mỗi 5 giây</li>
 * </ul>
 *
 * <p><b>HAI CHẾ ĐỘ HIỂN THỊ:</b>
 * <ol>
 *   <li>showingMyItems = false: tất cả phiên đấu giá (Bidder duyệt)</li>
 *   <li>showingMyItems = true: chỉ phiên của user đang đăng nhập (Seller quản lý)</li>
 * </ol>
 *
 * <p><b>QUYỀN HẠN UI ĐỘNG:</b>
 * <ul>
 *   <li>Bidder: chỉ thấy danh sách + xem chi tiết để bid</li>
 *   <li>Seller: thêm nút "Tạo phiên", "Sản phẩm của tôi"</li>
 *   <li>Admin: thêm nút "Quản trị"</li>
 * </ul>
 *
 * <p>Mọi truy cập dữ liệu đi qua {@link AuctionClient} → server. Realtime
 * update nhận qua {@link AuctionClientService.Listener} (push từ
 * server) thay vì observer-pattern in-process.
 */
public class DashboardController {

    // ===== UI ELEMENTS =====
    @FXML private Label pageTitle;            // Tiêu đề: "Danh sách phiên" hoặc "Sản phẩm của tôi"
    @FXML private Label userInfoLabel;        // Hiển thị tên + role user đang login
    @FXML private TextField searchField;      // Ô tìm theo tên sản phẩm
    @FXML private ComboBox<String> statusFilter; // Filter theo trạng thái phiên
    @FXML private FlowPane auctionFlowPane;   // Container chứa các card auction (tự xuống dòng)
    @FXML private ScrollPane scrollPane;      // Cho phép scroll khi nhiều card
    @FXML private Button btnCreateAuction;    // Nút tạo phiên (chỉ Seller/Admin)
    @FXML private Button btnMyItems;          // Nút xem sản phẩm của mình (Seller)
    @FXML private Button btnAdmin;            // Nút Admin panel (Admin)

    private final AuctionClientService auctionClientService = AuctionClientService.getInstance();

    /** Timer cục bộ cập nhật countdown trên card mỗi giây (không gọi server). */
    private Timer countdownTimer;

    /** Timer polling server mỗi 5s để đồng bộ dữ liệu mới. */
    private Timer syncTimer;

    /** Cache danh sách auction để hiển thị + filter local. */
    private List<Auction> currentAuctions = List.of();

    /** Cờ chuyển đổi giữa view "tất cả" và view "của tôi". */
    private boolean showingMyItems = false;

    /** Lưu tham chiếu các label countdown trên card để timer cập nhật cục bộ. */
    private final List<TimerLabelEntry> timerLabels = new ArrayList<>();

    private record TimerLabelEntry(Label label, Auction auction) {}

    /**
     * Listener nhận push event từ server (vd: bid mới ở phiên nào đó).
     *
     * <p>Khi có event đến → refreshAuctionList() để UI hiển thị giá mới nhất.
     * Phải dùng {@link Platform#runLater} vì thread network không được update UI
     * trực tiếp - chỉ JavaFX Application Thread mới được phép.
     */
    private final AuctionClientService.Listener serverListener = msg -> {
        Platform.runLater(() -> {
            // Kiểm tra controller còn được attach vào scene chưa (tránh memory leak)
            if (auctionFlowPane != null && auctionFlowPane.getScene() != null) {
                refreshAuctionList();
            }
        });
    };

    /**
     * Initialize - tự động chạy sau khi FXML load.
     *
     * <p>Setup:
     * <ol>
     *   <li>Kiểm tra user đã login (nếu chưa → quay về login)</li>
     *   <li>Hiển thị nút dựa trên role</li>
     *   <li>Setup filter + search</li>
     *   <li>Đăng ký listener push từ server</li>
     *   <li>Start timer auto-refresh 5s</li>
     *   <li>Đăng ký cleanup khi scene đổi</li>
     * </ol>
     */
    @FXML
    private void initialize() {
        // ===== Kiểm tra đã đăng nhập chưa =====
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            // Trường hợp bất thường (vd: scene navigate sai) → quay về login
            MainApp.navigateTo("/com/auction/view/login.fxml", "Đăng nhập");
            return;
        }

        // Hiển thị tên + role user trên góc trái
        userInfoLabel.setText(user.getFullName() + " (" + user.getRole().name() + ")");

        // ===== HIỂN THỊ NÚT THEO QUYỀN HẠN =====
        // Seller/Admin được tạo phiên + xem sản phẩm của mình
        if (user.getRole() == UserRole.SELLER || user.getRole() == UserRole.ADMIN) {
            btnCreateAuction.setVisible(true);
            btnCreateAuction.setManaged(true);
            btnMyItems.setVisible(true);
            btnMyItems.setManaged(true);
        }
        // Chỉ Admin mới thấy nút "Quản trị"
        if (user.getRole() == UserRole.ADMIN) {
            btnAdmin.setVisible(true);
            btnAdmin.setManaged(true);
        }

        // ===== Setup filter trạng thái =====
        statusFilter.getItems().addAll("Tất cả", "Đang diễn ra", "Sắp bắt đầu", "Đã kết thúc");
        statusFilter.getSelectionModel().selectFirst();
        // Đổi filter → refresh list
        statusFilter.setOnAction(e -> refreshAuctionList());

        // Listener: gõ chữ trong search → refresh ngay (live search)
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshAuctionList());

        // Đăng ký listener push từ server (BID_UPDATE / AUCTION_EVENT)
        auctionClientService.addPushListener(serverListener);

        // Load danh sách lần đầu
        refreshAuctionList();

        // Timer cục bộ 1s — chỉ cập nhật countdown trên card, KHÔNG gọi server.
        countdownTimer = new Timer(true);
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (auctionFlowPane == null || auctionFlowPane.getScene() == null) return;
                    updateCountdownLabels();
                });
            }
        }, 1000, 1000);

        // Timer polling 5s — gọi server lấy data mới (giá, trạng thái, bid count).
        syncTimer = new Timer(true);
        syncTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (auctionFlowPane == null || auctionFlowPane.getScene() == null) return;
                    refreshAuctionList();
                });
            }
        }, 5000, 5000);

        // Lắng nghe khi scene bị remove (user navigate đi nơi khác) → cleanup
        // để tránh memory leak (timer + listener vẫn chạy ngầm)
        auctionFlowPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) cleanup();
        });
    }

    /**
     * Dọn dẹp khi rời màn hình:
     * - Hủy timer auto-refresh
     * - Bỏ đăng ký listener push từ server
     * Tránh memory leak (timer chạy ngầm, listener giữ tham chiếu...).
     */
    private void cleanup() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
        if (syncTimer != null) {
            syncTimer.cancel();
            syncTimer = null;
        }
        auctionClientService.removePushListener(serverListener);
    }

    /**
     * Refresh toàn bộ danh sách auction trên UI.
     *
     * <p>Quy trình:
     * <ol>
     *   <li>Xóa toàn bộ card cũ</li>
     *   <li>Load auctions từ server</li>
     *   <li>Áp dụng filter (myItems / status / search)</li>
     *   <li>Tạo card mới cho mỗi auction</li>
     * </ol>
     */
    private void refreshAuctionList() {
        auctionFlowPane.getChildren().clear();
        timerLabels.clear();

        try {
            currentAuctions = auctionClientService.getAllAuctions();
        } catch (IOException e) {
            Label errLabel = new Label("Không tải được danh sách: " + e.getMessage());
            errLabel.setStyle("-fx-text-fill: #c0392b;");
            auctionFlowPane.getChildren().add(errLabel);
            return;
        }

        User user = SessionManager.getInstance().getCurrentUser();

        if (showingMyItems && user != null) {
            pageTitle.setText("Sản phẩm của tôi");
            List<Auction> myAuctions = currentAuctions.stream()
                    .filter(a -> user.getId().equals(a.getSellerId()))
                    .collect(Collectors.toList());
            if (myAuctions.isEmpty()) {
                Label emptyLabel = new Label("Bạn chưa có sản phẩm nào. Hãy tạo phiên đấu giá đầu tiên!");
                emptyLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 15px;");
                emptyLabel.setWrapText(true);
                auctionFlowPane.setAlignment(Pos.CENTER);
                auctionFlowPane.getChildren().add(emptyLabel);
            } else {
                auctionFlowPane.setAlignment(Pos.TOP_LEFT);
                for (Auction auction : myAuctions) {
                    auctionFlowPane.getChildren().add(createAuctionCard(auction, true));
                }
            }
            return;
        }

        pageTitle.setText("Danh sách phiên đấu giá");

        List<Auction> auctions = currentAuctions;

        String filter = statusFilter.getValue();
        if (filter != null && !filter.equals("Tất cả")) {
            auctions = auctions.stream().filter(a -> {
                if (filter.equals("Đang diễn ra")) return a.getStatus() == AuctionStatus.RUNNING;
                if (filter.equals("Sắp bắt đầu")) return a.getStatus() == AuctionStatus.OPEN;
                if (filter.equals("Đã kết thúc"))
                    return a.getStatus() == AuctionStatus.FINISHED
                            || a.getStatus() == AuctionStatus.PAID
                            || a.getStatus() == AuctionStatus.CANCELED;
                return true;
            }).collect(Collectors.toList());
        }

        String search = searchField.getText();
        if (search != null && !search.trim().isEmpty()) {
            String lowerSearch = search.toLowerCase();
            auctions = auctions.stream()
                    .filter(a -> a.getItemName().toLowerCase().contains(lowerSearch))
                    .collect(Collectors.toList());
        }

        if (auctions.isEmpty()) {
            Label emptyLabel = new Label("Không có phiên đấu giá nào");
            emptyLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 16px;");
            auctionFlowPane.setAlignment(Pos.CENTER);
            auctionFlowPane.getChildren().add(emptyLabel);
            return;
        }

        auctionFlowPane.setAlignment(Pos.TOP_LEFT);
        String currentUserId = user != null ? user.getId() : "";
        for (Auction auction : auctions) {
            boolean isSeller = auction.getSellerId().equals(currentUserId);
            auctionFlowPane.getChildren().add(createAuctionCard(auction, isSeller));
        }
    }

    /**
     * Tạo 1 card UI hiển thị thông tin của 1 auction.
     *
     * <p>Cấu trúc card (từ trên xuống):
     * <ol>
     *   <li>Badge trạng thái (màu khác nhau theo status)</li>
     *   <li>Tên sản phẩm</li>
     *   <li>Giá hiện tại</li>
     *   <li>Countdown timer (nếu RUNNING) hoặc giờ bắt đầu</li>
     *   <li>Số lượt bid</li>
     *   <li>(Nếu là seller) Nút "Chỉnh sửa"</li>
     * </ol>
     *
     * <p>Click vào card → chuyển sang màn hình chi tiết phiên đấu giá.
     *
     * @param auction        Auction cần hiển thị
     * @param showEditButton có hiển thị nút Edit không (true nếu user là seller của item)
     * @return VBox card đã build sẵn
     */
    private VBox createAuctionCard(Auction auction, boolean showEditButton) {
        VBox card = new VBox(10);
        card.getStyleClass().add("auction-card");
        card.setPrefWidth(320);
        card.setMinWidth(300);
        card.setCursor(javafx.scene.Cursor.HAND);

        Label statusLabel = new Label(auction.getStatus().getDisplayName());
        switch (auction.getStatus()) {
            case RUNNING  -> statusLabel.getStyleClass().add("status-running");
            case OPEN     -> statusLabel.getStyleClass().add("status-open");
            case FINISHED, PAID -> statusLabel.getStyleClass().add("status-finished");
            case CANCELED -> statusLabel.getStyleClass().add("status-canceled");
        }

        Label nameLabel = new Label(auction.getItemName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        nameLabel.setWrapText(true);

        Label priceLabel = new Label(String.format("%,.0f VNĐ", auction.getCurrentHighestBid()));
        priceLabel.getStyleClass().add("price-small");

        Label timerLabel = new Label();
        if (auction.getStatus() == AuctionStatus.RUNNING) {
            Duration remaining = Duration.between(LocalDateTime.now(), auction.getEndTime());
            if (remaining.isNegative()) {
                timerLabel.setText("Đã hết giờ");
            } else {
                long hours   = remaining.toHours();
                long minutes = remaining.toMinutesPart();
                long seconds = remaining.toSecondsPart();
                timerLabel.setText(String.format("Còn %02d:%02d:%02d", hours, minutes, seconds));
            }
            timerLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
        } else if (auction.getStatus() == AuctionStatus.OPEN) {
            timerLabel.setText("Bắt đầu: " +
                    auction.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
            timerLabel.setStyle("-fx-text-fill: #2980b9;");
        } else {
            timerLabel.setText("Kết thúc");
            timerLabel.setStyle("-fx-text-fill: #888;");
        }

        Label bidsLabel = new Label(auction.getTotalBids() + " lượt đặt giá");
        bidsLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

        card.getChildren().addAll(statusLabel, nameLabel, priceLabel, timerLabel, bidsLabel);

        if (showEditButton) {
            Button editBtn = new Button("✏ Chỉnh sửa sản phẩm");
            editBtn.setStyle("-fx-font-size: 12px; -fx-background-color: #f39c12; "
                    + "-fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 12 5 12;");
            editBtn.setMaxWidth(Double.MAX_VALUE);
            editBtn.setOnAction(e -> {
                e.consume();
                openEditItem(auction.getItemId());
            });
            card.getChildren().add(editBtn);
        }

        if (auction.getStatus() == AuctionStatus.RUNNING) {
            timerLabels.add(new TimerLabelEntry(timerLabel, auction));
        }

        card.setOnMouseClicked(e -> openAuctionDetail(auction.getId()));

        return card;
    }

    /** Cập nhật countdown cục bộ trên các card RUNNING — không gọi server. */
    private void updateCountdownLabels() {
        for (TimerLabelEntry entry : timerLabels) {
            Duration remaining = Duration.between(LocalDateTime.now(), entry.auction().getEndTime());
            if (remaining.isNegative()) {
                entry.label().setText("Đã hết giờ");
            } else {
                entry.label().setText(String.format("Còn %02d:%02d:%02d",
                        remaining.toHours(), remaining.toMinutesPart(), remaining.toSecondsPart()));
            }
        }
    }

    /**
     * Mở màn hình EditItem cho 1 item cụ thể.
     *
     * <p>Khác với navigateTo() thông thường: phải lấy controller sau khi load
     * FXML để truyền Item vào (setItem). Đây là pattern "FXMLLoader + controller
     * injection" - dùng khi cần truyền data tới controller mới.
     */
    private void openEditItem(String itemId) {
        try {
            Item item = auctionClientService.getItem(itemId);
            FXMLLoader loader = MainApp.getLoader("/com/auction/view/edit_item.fxml");
            Parent root = loader.load();
            EditItemController controller = loader.getController();
            controller.setItem(item);
            Scene scene = new Scene(root, 1100, 750);
            scene.getStylesheets().add(
                    MainApp.class.getResource("/com/auction/css/style.css").toExternalForm());
            MainApp.getPrimaryStage().setScene(scene);
        } catch (IOException e) {
            new Alert(Alert.AlertType.WARNING,
                    "Không tải được sản phẩm: " + e.getMessage(), ButtonType.OK).showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Mở màn hình AuctionDetail cho 1 phiên cụ thể.
     * Tương tự openEditItem - truyền auctionId vào controller mới.
     */
    private void openAuctionDetail(String auctionId) {
        try {
            FXMLLoader loader = MainApp.getLoader("/com/auction/view/auction_detail.fxml");
            Parent root = loader.load();
            AuctionDetailController controller = loader.getController();
            controller.setAuctionId(auctionId);
            Scene scene = new Scene(root, 1100, 750);
            scene.getStylesheets().add(
                    MainApp.class.getResource("/com/auction/css/style.css").toExternalForm());
            MainApp.getPrimaryStage().setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========================================================================
    // CÁC HANDLER CHO NÚT TRÊN TOOLBAR
    // ========================================================================

    /** Chuyển sang view "Tất cả phiên". */
    @FXML
    private void handleShowAuctions() {
        showingMyItems = false;
        refreshAuctionList();
    }

    /** Chuyển sang view "Sản phẩm của tôi" - chỉ Seller dùng. */
    @FXML
    private void handleShowMyItems() {
        showingMyItems = true;
        refreshAuctionList();
    }

    /** Mở admin panel - chỉ Admin có nút này. */
    @FXML
    private void handleShowAdmin() {
        MainApp.navigateTo("/com/auction/view/admin.fxml", "Quản trị");
    }

    /** Mở màn hình tạo phiên đấu giá - chỉ Seller/Admin có nút này. */
    @FXML
    private void handleCreateAuction() {
        MainApp.navigateTo("/com/auction/view/create_auction.fxml", "Tạo phiên đấu giá");
    }

    /**
     * Logout - dọn dẹp, báo server, xóa session, về login.
     *
     * <p>Quy trình logout HOÀN CHỈNH:
     * <ol>
     *   <li>cleanup(): dừng timer + listener</li>
     *   <li>Gọi server logout (server xóa session)</li>
     *   <li>Xóa session local</li>
     *   <li>Navigate về login</li>
     * </ol>
     */
    @FXML
    private void handleLogout() {
        cleanup();
        try {
            auctionClientService.logout();
        } catch (IOException ignored) {
            // Bỏ qua lỗi network khi logout (đang thoát rồi)
        }
        SessionManager.getInstance().logout();
        MainApp.navigateTo("/com/auction/view/login.fxml", "Đăng nhập");
    }
}
