package com.auction.controller;

import com.auction.MainApp;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.user.User;
import com.auction.network.AuctionClient;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;

/**
 * Controller cho Admin panel — qua server.
 */
public class AdminController {

    @FXML private Label totalUsersLabel;
    @FXML private Label totalAuctionsLabel;
    @FXML private Label activeAuctionsLabel;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colAction;

    private final AuctionClient client = AuctionClient.getInstance();

    @FXML
    private void initialize() {
        setupTable();
        loadData();
    }

    private void setupTable() {
        colUsername.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUsername()));
        colFullName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFullName()));
        colEmail.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmail()));
        colRole.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRole().name()));
        colStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isActive() ? "Hoạt động" : "Đã khóa"));

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Khóa tài khoản");
            {
                btn.getStyleClass().add("btn-danger");
                btn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10 4 10;");
                btn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    try {
                        client.deactivateUser(user.getId());
                        loadData();
                    } catch (IOException ex) {
                        new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    btn.setDisable(!user.isActive());
                    btn.setText(user.isActive() ? "Khóa" : "Đã khóa");
                    setGraphic(btn);
                }
            }
        });
    }

    private void loadData() {
        try {
            List<User> users = client.getAllUsers();
            List<Auction> allAuctions = client.getAllAuctions();
            long activeCount = allAuctions.stream()
                    .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                    .count();

            totalUsersLabel.setText(String.valueOf(users.size()));
            totalAuctionsLabel.setText(String.valueOf(allAuctions.size()));
            activeAuctionsLabel.setText(String.valueOf(activeCount));

            ObservableList<User> userList = FXCollections.observableArrayList(users);
            usersTable.setItems(userList);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Không tải được dữ liệu: " + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleBack() {
        MainApp.navigateTo("/com/auction/view/dashboard.fxml", "Trang chủ");
    }
}
