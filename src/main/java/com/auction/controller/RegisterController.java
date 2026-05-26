package com.auction.controller;

import com.auction.MainApp;
import com.auction.model.user.UserRole;
import com.auction.network.AuctionClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;

/**
 * Controller cho màn hình đăng ký. Đăng ký đi qua server.
 */
public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private final AuctionClient client = AuctionClient.getInstance();

    @FXML
    private void initialize() {
        roleComboBox.getItems().addAll("Người mua (Bidder)", "Người bán (Seller)");
        roleComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleRegister() {
        hideMessages();

        String username = usernameField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String roleStr = roleComboBox.getValue();

        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Vui lòng điền đầy đủ thông tin");
            return;
        }

        UserRole role = roleStr.contains("Seller") ? UserRole.SELLER : UserRole.BIDDER;

        try {
            client.register(username, password, email, fullName, role.name());
            showSuccess("Đăng ký thành công! Đang chuyển đến đăng nhập...");

            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(
                            () -> MainApp.navigateTo("/com/auction/view/login.fxml", "Đăng nhập"));
                } catch (InterruptedException ignored) {}
            }).start();

        } catch (IOException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleGoToLogin() {
        MainApp.navigateTo("/com/auction/view/login.fxml", "Đăng nhập");
    }

    private void hideMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }
}
