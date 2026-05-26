package com.auction.controller;

import com.auction.MainApp;
import com.auction.SessionManager;
import com.auction.model.user.User;
import com.auction.network.client.AuctionClient;
import com.auction.network.client.AuctionClientService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * ============================================================================
 * LOGINCONTROLLER - CONTROLLER MÀN HÌNH ĐĂNG NHẬP
 * ============================================================================
 *
 * <p>Controller này điều khiển màn hình login (file FXML:
 * {@code /com/auction/view/login.fxml}). Quy trình:
 * <ol>
 *   <li>User nhập username + password → bấm "Đăng nhập"</li>
 *   <li>Controller gửi request LOGIN qua {@link AuctionClient} tới server</li>
 *   <li>Server xác thực → trả về User nếu OK</li>
 *   <li>Lưu User vào {@link SessionManager}</li>
 *   <li>Chuyển sang màn hình Dashboard</li>
 * </ol>
 *
 * <p><b>JAVAFX FXML CONTROLLER PATTERN:</b>
 * <ul>
 *   <li>FXML định nghĩa UI (XML)</li>
 *   <li>Controller xử lý sự kiện UI (Java)</li>
 *   <li>Annotation {@code @FXML} link UI element trong FXML với field trong controller</li>
 *   <li>Tên field PHẢI khớp với fx:id trong FXML</li>
 * </ul>
 */
public class LoginController {

    // ===== CÁC UI ELEMENT TỪ FILE FXML =====
    // @FXML cho phép FXML loader gán giá trị vào field private
    // Tên biến phải KHỚP với fx:id trong file login.fxml

    /** Ô nhập username. */
    @FXML private TextField usernameField;

    /** Ô nhập password (ẩn ký tự). */
    @FXML private PasswordField passwordField;

    /** Label hiển thị lỗi (ẩn ban đầu). */
    @FXML private Label errorLabel;

    /** Service truy cập client - dùng chung trong toàn UI. */
    private final AuctionClientService auctionClientService = AuctionClientService.getInstance();

    /**
     * Xử lý sự kiện bấm nút "Đăng nhập".
     *
     * <p>Phương thức được gọi tự động khi user bấm nút (link qua FXML
     * {@code onAction="#handleLogin"}). Annotation @FXML là bắt buộc khi
     * method là private.
     */
    @FXML
    private void handleLogin() {
        // Lấy dữ liệu từ form, trim để bỏ space thừa
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validate đơn giản
        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        try {
            // Gọi server qua TCP để xác thực
            User user = auctionClientService.login(username, password);

            // Lưu user vào session (Singleton) - các màn hình sau dùng được
            SessionManager.getInstance().setCurrentUser(user);

            // Chuyển sang Dashboard
            MainApp.navigateTo("/com/auction/view/dashboard.fxml", "Trang chủ");
        } catch (IOException e) {
            // Lỗi mạng hoặc xác thực fail → hiển thị message
            showError(e.getMessage());
        }
    }

    /**
     * Chuyển sang màn hình đăng ký khi user bấm "Đăng ký tài khoản mới".
     */
    @FXML
    private void handleGoToRegister() {
        MainApp.navigateTo("/com/auction/view/register.fxml", "Đăng ký");
    }

    /**
     * Hiển thị thông báo lỗi trong errorLabel.
     *
     * <p>setVisible(true) + setManaged(true) để label vừa hiện vừa chiếm chỗ
     * trong layout. Nếu chỉ setVisible(true) mà không setManaged → label sẽ
     * không chiếm space, layout giữ nguyên.
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
