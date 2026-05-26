package com.auction.controller;

import com.auction.MainApp;
import com.auction.SessionManager;
import com.auction.model.user.User;
import com.auction.network.AuctionClient;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * Controller cho màn hình đăng nhập.
 * Mọi thao tác xác thực đi qua {@link AuctionClient} → server.
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuctionClient client = AuctionClient.getInstance();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        try {
            User user = client.login(username, password);
            SessionManager.getInstance().setCurrentUser(user);
            MainApp.navigateTo("/com/auction/view/dashboard.fxml", "Trang chủ");
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleGoToRegister() {
        MainApp.navigateTo("/com/auction/view/register.fxml", "Đăng ký");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
