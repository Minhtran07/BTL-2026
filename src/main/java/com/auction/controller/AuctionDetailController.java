package com.auction.controller;

import com.auction.MainApp;
import com.auction.SessionManager;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.transaction.BidTransaction;
import com.auction.model.user.User;
import com.auction.network.AuctionClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Controller cho màn hình chi tiết phiên đấu giá — qua server.
 *
 * <p>Cập nhật realtime:
 * <ul>
 *   <li>{@link AuctionClient.PushListener} nhận push BID_UPDATE /
 *       AUCTION_EVENT từ server và refresh UI.</li>
 *   <li>Sync timer 2s vẫn chạy để cập nhật countdown timer + bù trường hợp
 *       miss event.</li>
 * </ul>
 */
public class AuctionDetailController {

    @FXML private Label userInfoLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label categoryLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label startPriceLabel;
    @FXML private Label sellerLabel;
    @FXML private Label statusLabel;
    @FXML private Label timerLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Label totalBidsLabel;
    @FXML private Label antiSnipeLabel;
    @FXML private Label bidErrorLabel;
    @FXML private Label bidSuccessLabel;
    @FXML private TextField bidAmountField;
    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;
    @FXML private Button btnPlaceBid;
    @FXML private ListView<String> bidHistoryList;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private String auctionId;
    private final AuctionClient client = AuctionClient.getInstance();
    private Auction currentAuction;
    private Timer countdownTimer;
    private Timer syncTimer;
    private XYChart.Series<String, Number> priceSeries;
    private String lastChartFingerprint;

    /** Lắng nghe BID_UPDATE / AUCTION_EVENT đẩy từ server. */
    private final AuctionClient.PushListener serverListener = msg -> {
        // Chỉ refresh nếu event liên quan đến phiên này
        String eventAuctionId = msg.get("auctionId");
        if (eventAuctionId != null && eventAuctionId.equals(auctionId)) {
            Platform.runLater(this::reloadAuction);
        }
    };

    @FXML
    private void initialize() {
        attachThousandSeparatorFormatter(bidAmountField);
        attachThousandSeparatorFormatter(maxBidField);
        attachThousandSeparatorFormatter(incrementField);
    }

    private void attachThousandSeparatorFormatter(TextField field) {
        if (field == null) return;
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            int caretBefore = field.getCaretPosition();
            int digitsBeforeCaret = 0;
            for (int i = 0; i < Math.min(caretBefore, newVal.length()); i++) {
                if (Character.isDigit(newVal.charAt(i))) digitsBeforeCaret++;
            }
            String digits = newVal.replaceAll("\\D", "");
            String formatted;
            if (digits.isEmpty()) {
                formatted = "";
            } else {
                try {
                    long value = Long.parseLong(digits);
                    formatted = String.format(java.util.Locale.US, "%,d", value);
                } catch (NumberFormatException e) {
                    formatted = digits;
                }
            }
            if (formatted.equals(newVal)) return;
            field.setText(formatted);
            int newCaret = 0;
            int seenDigits = 0;
            for (int i = 0; i < formatted.length() && seenDigits < digitsBeforeCaret; i++) {
                if (Character.isDigit(formatted.charAt(i))) seenDigits++;
                newCaret = i + 1;
            }
            field.positionCaret(newCaret);
        });
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
        client.addPushListener(serverListener);
        reloadAuction();
        startCountdown();
        startSyncTimer();
    }

    /** Tải lại auction từ server. */
    private void reloadAuction() {
        try {
            currentAuction = client.getAuction(auctionId);
            loadAuctionData();
        } catch (IOException e) {
            // ignore — UI giữ trạng thái cũ, sync timer sẽ thử lại
            System.err.println("[AuctionDetail] Không tải được phiên: " + e.getMessage());
        }
    }

    private void startSyncTimer() {
        syncTimer = new Timer(true);
        syncTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    try {
                        if (currentPriceLabel == null || currentPriceLabel.getScene() == null) return;
                        reloadAuction();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }, 2000, 2000);
    }

    private void loadAuctionData() {
        if (currentAuction == null) return;
        Auction auction = currentAuction;

        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            userInfoLabel.setText(user.getFullName());
        }

        itemNameLabel.setText(auction.getItemName());
        categoryLabel.setText("Phiên đấu giá #" + auction.getId().substring(0, 8));
        descriptionLabel.setText("Sản phẩm đấu giá với giá khởi điểm " +
                String.format("%,.0f VNĐ", auction.getStartingPrice()));
        startPriceLabel.setText(String.format("%,.0f VNĐ", auction.getStartingPrice()));
        sellerLabel.setText("ID: " + auction.getSellerId().substring(0, 8));

        statusLabel.setText(auction.getStatus().getDisplayName());
        statusLabel.getStyleClass().removeAll("status-running", "status-open", "status-finished", "status-canceled");
        switch (auction.getStatus()) {
            case RUNNING -> statusLabel.getStyleClass().add("status-running");
            case OPEN -> statusLabel.getStyleClass().add("status-open");
            case FINISHED, PAID -> statusLabel.getStyleClass().add("status-finished");
            case CANCELED -> statusLabel.getStyleClass().add("status-canceled");
        }

        currentPriceLabel.setText(String.format("%,.0f VNĐ", auction.getCurrentHighestBid()));
        if (auction.getCurrentHighestBidderName() != null) {
            highestBidderLabel.setText("Người dẫn đầu: " + auction.getCurrentHighestBidderName());
        } else {
            highestBidderLabel.setText("Chưa có ai đặt giá");
        }

        totalBidsLabel.setText(String.valueOf(auction.getTotalBids()));
        antiSnipeLabel.setText(auction.isAntiSnipingEnabled() ?
                "Bật (" + auction.getSnipeExtensionCount() + " lần gia hạn)" : "Tắt");

        boolean canBid = auction.getStatus() == AuctionStatus.RUNNING;
        btnPlaceBid.setDisable(!canBid);
        bidAmountField.setDisable(!canBid);

        updateBidHistory(auction);
        updatePriceChart(auction);

        if (bidAmountField.getText().isEmpty() && canBid) {
            double suggestedBid = auction.getCurrentHighestBid() * 1.05;
            bidAmountField.setPromptText(String.format("Gợi ý: %,.0f", suggestedBid));
        }
    }

    private void updateBidHistory(Auction auction) {
        ObservableList<String> items = FXCollections.observableArrayList();
        List<BidTransaction> history = auction.getBidHistory();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        for (int i = history.size() - 1; i >= 0; i--) {
            BidTransaction tx = history.get(i);
            items.add(String.format("[%s] %s — %,.0f VNĐ",
                    tx.getBidTime().format(fmt),
                    tx.getBidderName(),
                    tx.getBidAmount()));
        }
        bidHistoryList.setItems(items);
    }

    private void updatePriceChart(Auction auction) {
        List<BidTransaction> history = auction.getBidHistory();

        String currentFingerprint = computeHistoryFingerprint(history);
        if (priceSeries != null
                && priceChart.getData().contains(priceSeries)
                && currentFingerprint.equals(lastChartFingerprint)) {
            return;
        }

        XYChart.Series<String, Number> newSeries = new XYChart.Series<>();
        newSeries.setName("Giá đấu");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        newSeries.getData().add(new XYChart.Data<>("Khởi điểm", auction.getStartingPrice()));

        java.util.Map<String, Integer> seen = new java.util.HashMap<>();
        for (BidTransaction tx : history) {
            String base  = tx.getBidTime().format(fmt);
            int    count = seen.merge(base, 1, Integer::sum);
            String label = (count == 1) ? base : base + " #" + count;
            newSeries.getData().add(new XYChart.Data<>(label, tx.getBidAmount()));
        }

        priceChart.getData().clear();
        priceChart.getData().add(newSeries);
        priceSeries = newSeries;
        lastChartFingerprint = currentFingerprint;
    }

    private String computeHistoryFingerprint(List<BidTransaction> history) {
        if (history.isEmpty()) return "0";
        int hash = 1;
        for (BidTransaction tx : history) {
            hash = 31 * hash + tx.getId().hashCode();
            hash = 31 * hash + Double.hashCode(tx.getBidAmount());
        }
        return history.size() + ":" + hash;
    }

    private void startCountdown() {
        countdownTimer = new Timer(true);
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (currentAuction == null) return;
                    Auction auction = currentAuction;
                    if (auction.getStatus() == AuctionStatus.RUNNING) {
                        Duration remaining = Duration.between(LocalDateTime.now(), auction.getEndTime());
                        if (remaining.isNegative()) {
                            timerLabel.setText("00:00:00");
                        } else {
                            timerLabel.setText(String.format("%02d:%02d:%02d",
                                    remaining.toHours(),
                                    remaining.toMinutesPart(),
                                    remaining.toSecondsPart()));
                        }
                    } else if (auction.getStatus() == AuctionStatus.FINISHED) {
                        timerLabel.setText("Đã kết thúc");
                        countdownTimer.cancel();
                    } else if (auction.getStatus() == AuctionStatus.OPEN) {
                        timerLabel.setText("Chưa bắt đầu");
                    }
                });
            }
        }, 0, 1000);
    }

    @FXML
    private void handlePlaceBid() {
        hideBidMessages();
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        String amountText = stripFormatting(bidAmountField.getText());
        if (amountText.isEmpty()) {
            showBidError("Vui lòng nhập số tiền");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            client.placeBid(auctionId, amount);
            showBidSuccess(String.format("Đặt giá thành công: %,.0f VNĐ", amount));
            bidAmountField.clear();
            reloadAuction();
        } catch (NumberFormatException e) {
            showBidError("Số tiền không hợp lệ");
        } catch (IOException e) {
            showBidError(e.getMessage());
        }
    }

    private String stripFormatting(String text) {
        if (text == null) return "";
        return text.replaceAll("\\D", "");
    }

    @FXML
    private void handleAutoBid() {
        hideBidMessages();
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        try {
            String maxText = stripFormatting(maxBidField.getText());
            String incText = stripFormatting(incrementField.getText());

            if (maxText.isEmpty() || incText.isEmpty()) {
                showBidError("Vui lòng nhập giá tối đa và bước giá");
                return;
            }

            double maxBid = Double.parseDouble(maxText);
            double increment = Double.parseDouble(incText);

            client.registerAutoBid(auctionId, maxBid, increment);
            showBidSuccess("Auto-Bid đã được kích hoạt!");
            maxBidField.clear();
            incrementField.clear();
            reloadAuction();
        } catch (NumberFormatException e) {
            showBidError("Số tiền không hợp lệ");
        } catch (IOException e) {
            showBidError(e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        cleanup();
        MainApp.navigateTo("/com/auction/view/dashboard.fxml", "Trang chủ");
    }

    private void cleanup() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
        if (syncTimer != null) {
            syncTimer.cancel();
            syncTimer = null;
        }
        client.removePushListener(serverListener);
    }

    private void hideBidMessages() {
        bidErrorLabel.setVisible(false);
        bidErrorLabel.setManaged(false);
        bidSuccessLabel.setVisible(false);
        bidSuccessLabel.setManaged(false);
    }

    private void showBidError(String msg) {
        bidErrorLabel.setText(msg);
        bidErrorLabel.setVisible(true);
        bidErrorLabel.setManaged(true);
    }

    private void showBidSuccess(String msg) {
        bidSuccessLabel.setText(msg);
        bidSuccessLabel.setVisible(true);
        bidSuccessLabel.setManaged(true);
    }
}
