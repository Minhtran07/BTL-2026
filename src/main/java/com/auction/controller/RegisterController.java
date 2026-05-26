package com.auction.controller;

import com.auction.MainApp;
import com.auction.model.user.UserRole;
import com.auction.network.client.AuctionClientService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * ============================================================================
 * REGISTERCONTROLLER - CONTROLLER MÀN HÌNH ĐĂNG KÝ
 * ============================================================================
 *
 * <p>Controller xử lý màn hình đăng ký tài khoản mới. User nhập thông tin →
 * gửi lên server → server tạo user mới trong DB.
 *
 * <p><b>Lưu ý:</b> không cho phép đăng ký Admin qua UI (chỉ Bidder/Seller).
 * Admin phải tạo trực tiếp trong DB (script seed hoặc SQL command).
 */
public class RegisterController {

    // ===== UI Elements từ FXML =====
    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private final AuctionClientService auctionClientService = AuctionClientService.getInstance();

    /**
     * Khởi tạo controller - được gọi sau khi FXML load xong (auto by JavaFX).
     * Set giá trị mặc định cho ComboBox role.
     */
    @FXML
    private void initialize() {
        // Thêm 2 lựa chọn vào dropdown role
        roleComboBox.getItems().addAll("Người mua (Bidder)", "Người bán (Seller)");
        // Mặc định chọn item đầu tiên (Bidder)
        roleComboBox.getSelectionModel().selectFirst();
    }

    /**
     * Xử lý sự kiện bấm "Đăng ký".
     * Validate input → gọi server đăng ký → nếu OK thì delay 1.5s rồi chuyển về login.
     */
    @FXML
    private void handleRegister() {
        hideMessages(); // Xóa thông báo cũ

        // Lấy data từ form
        String username = usernameField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String roleStr = roleComboBox.getValue();

        // Validate: tất cả field bắt buộc
        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Vui lòng điền đầy đủ thông tin");
            return;
        }

        // Chuyển String "Người bán (Seller)" → enum UserRole.SELLER
        UserRole role = roleStr.contains("Seller") ? UserRole.SELLER : UserRole.BIDDER;

        try {
            // Gọi server đăng ký
            auctionClientService.register(username, password, email, fullName, role.name());
            showSuccess("Đăng ký thành công! Đang chuyển đến đăng nhập...");

            // Tạo thread nền để delay 1.5s rồi chuyển màn hình
            // KHÔNG dùng Thread.sleep() trong UI thread (sẽ block UI!)
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    // QUAN TRỌNG: phải dùng Platform.runLater() để chạy code UI
                    // trên JavaFX Application Thread (không được update UI từ thread khác)
                    javafx.application.Platform.runLater(
                            () -> MainApp.navigateTo("/com/auction/view/login.fxml", "Đăng nhập"));
                } catch (InterruptedException ignored) {
                    // Bị interrupt - bỏ qua
                }
            }).start();

        } catch (IOException e) {
            // Lỗi mạng hoặc trùng username/email
            showError(e.getMessage());
        }
    }

    /** Quay lại màn hình đăng nhập. */
    @FXML
    private void handleGoToLogin() {
        MainApp.navigateTo("/com/auction/view/login.fxml", "Đăng nhập");
    }

    /** Ẩn cả label lỗi và label thành công. */
    private void hideMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    /** Hiển thị thông báo lỗi (đỏ). */
    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    /** Hiển thị thông báo thành công (xanh). */
    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }
}
