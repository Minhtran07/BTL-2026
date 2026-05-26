package com.auction.controller;

import com.auction.MainApp;
import com.auction.SessionManager;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.AuctionClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

/**
 * Controller cho Dashboard — danh sách phiên đấu giá.
 *
 * <p>Mọi truy cập dữ liệu đi qua {@link AuctionClient} → server. Realtime
 * update nhận qua {@link AuctionClient.PushListener} (push từ
 * server) thay vì observer-pattern in-process.
 */
public class DashboardController {

    @FXML private Label pageTitle;
    @FXML private Label userInfoLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private FlowPane auctionFlowPane;
    @FXML private ScrollPane scrollPane;
    @FXML private Button btnCreateAuction;
    @FXML private Button btnMyItems;
    @FXML private Button btnAdmin;

    private final AuctionClient client = AuctionClient.getInstance();
    private Timer refreshTimer;
    private List<Auction> currentAuctions = List.of();

    private boolean showingMyItems = false;

    /** Listener push từ server: refresh khi có event/bid update. */
    private final AuctionClient.PushListener serverListener = msg -> {
        Platform.runLater(() -> {
            if (auctionFlowPane != null && auctionFlowPane.getScene() != null) {
                refreshAuctionList();
            }
        });
    };

    @FXML
    private void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            MainApp.navigateTo("/com/auction/view/login.fxml", "Đăng nhập");
            return;
        }

        userInfoLabel.setText(user.getFullName() + " (" + user.getRole().name() + ")");

        if (user.getRole() == UserRole.SELLER || user.getRole() == UserRole.ADMIN) {
            btnCreateAuction.setVisible(true);
            btnCreateAuction.setManaged(true);
            btnMyItems.setVisible(true);
            btnMyItems.setManaged(true);
        }
        if (user.getRole() == UserRole.ADMIN) {
            btnAdmin.setVisible(true);
            btnAdmin.setManaged(true);
        }

        statusFilter.getItems().addAll("Tất cả", "Đang diễn ra", "Sắp bắt đầu", "Đã kết thúc");
        statusFilter.getSelectionModel().selectFirst();
        statusFilter.setOnAction(e -> refreshAuctionList());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshAuctionList());

        // Lắng nghe event từ server (BID_UPDATE / AUCTION_EVENT)
        client.addPushListener(serverListener);

        refreshAuctionList();

        // Auto-refresh để cập nhật countdown timer
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    try {
                        if (auctionFlowPane != null && auctionFlowPane.getScene() != null) {
                            refreshAuctionList();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }, 5000, 5000);

        auctionFlowPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) cleanup();
        });
    }

    private void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
        client.removePushListener(serverListener);
    }

    private void refreshAuctionList() {
        auctionFlowPane.getChildren().clear();

        try {
            currentAuctions = client.getAllAuctions();
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

        card.setOnMouseClicked(e -> openAuctionDetail(auction.getId()));

        return card;
    }

    private void openEditItem(String itemId) {
        try {
            Item item = client.getItem(itemId);
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

    @FXML
    private void handleShowAuctions() {
        showingMyItems = false;
        refreshAuctionList();
    }

    @FXML
    private void handleShowMyItems() {
        showingMyItems = true;
        refreshAuctionList();
    }

    @FXML
    private void handleShowAdmin() {
        MainApp.navigateTo("/com/auction/view/admin.fxml", "Quản trị");
    }

    @FXML
    private void handleCreateAuction() {
        MainApp.navigateTo("/com/auction/view/create_auction.fxml", "Tạo phiên đấu giá");
    }

    @FXML
    private void handleLogout() {
        cleanup();
        try {
            client.logout();
        } catch (IOException ignored) {}
        SessionManager.getInstance().logout();
        MainApp.navigateTo("/com/auction/view/login.fxml", "Đăng nhập");
    }
}
