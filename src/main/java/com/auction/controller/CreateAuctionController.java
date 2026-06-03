package com.auction.controller;

import com.auction.MainApp;
import com.auction.SessionManager;
import com.auction.model.item.ItemCategory;
import com.auction.model.user.User;
import com.auction.network.client.AuctionClientService;
import com.auction.pattern.strategy.CategoryFormRegistry;
import com.auction.pattern.strategy.CategoryFormStrategy;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.Map;

/**
 * ============================================================================
 * CREATEAUCTIONCONTROLLER - CONTROLLER MÀN HÌNH TẠO PHIÊN ĐẤU GIÁ MỚI
 * ============================================================================
 *
 * <p>Cho phép Seller tạo Item + Auction trong cùng 1 form. Tính năng nổi bật:
 * <ul>
 *   <li>Form ĐỘNG: chọn category → tự động hiển thị các field phù hợp
 *       (Electronics → brand, model, condition; Art → artist, year, medium...)</li>
 *   <li>2 round-trip lên server: tạo Item trước → lấy itemId → tạo Auction</li>
 * </ul>
 *
 * <p><b>Strategy Pattern:</b> Mỗi danh mục sản phẩm có một
 * {@link CategoryFormStrategy} riêng, chịu trách nhiệm tạo các field động
 * và thu thập dữ liệu. Controller không cần biết chi tiết từng category.
 *
 * <p><b>Lưu ý:</b> Chỉ Seller mới truy cập được màn hình này (kiểm tra ở Dashboard).
 */
public class CreateAuctionController {

    // ===== UI Elements cố định =====
    @FXML private TextField itemNameField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextArea descriptionArea;
    @FXML private TextField startPriceField;
    @FXML private TextField durationField; // thời lượng phiên (phút)
    /** Container chứa các field thay đổi theo category (động). */
    @FXML private VBox extraFieldsContainer;
    @FXML private Label errorLabel;

    private final AuctionClientService auctionClientService = AuctionClientService.getInstance();

    /** Strategy hiện tại - quyết định field nào hiển thị và cách thu thập data. */
    private CategoryFormStrategy currentStrategy;

    /**
     * Initialize - chạy sau khi FXML load.
     * Setup ComboBox category + listener thay đổi strategy theo lựa chọn.
     */
    @FXML
    private void initialize() {
        // Lấy danh sách category từ registry — không hardcode
        categoryComboBox.getItems().addAll(CategoryFormRegistry.getRegisteredNames());
        categoryComboBox.getSelectionModel().selectFirst();
        // Mỗi khi đổi lựa chọn → đổi strategy → rebuild field
        categoryComboBox.setOnAction(e -> applyStrategy());
        applyStrategy(); // hiển thị field cho category mặc định
    }

    /**
     * Áp dụng strategy theo category đã chọn.
     *
     * <p><b>Strategy Pattern:</b> thay vì if-else kiểm tra category rồi tạo
     * field thủ công, controller chỉ cần gọi {@code strategy.buildFields()}.
     * Khi thêm danh mục mới, chỉ tạo class strategy mới mà không sửa controller.
     */
    private void applyStrategy() {
        extraFieldsContainer.getChildren().clear();
        String categoryName = categoryComboBox.getValue();
        currentStrategy = CategoryFormRegistry.of(categoryName);
        currentStrategy.buildFields(extraFieldsContainer);
    }

    /**
     * Xử lý nút "Tạo phiên đấu giá".
     * Validate → tạo Item → tạo Auction → về Dashboard.
     */
    @FXML
    private void handleCreate() {
        hideError();
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return; // chưa đăng nhập

        // Lấy data từ form
        String name = itemNameField.getText().trim();
        String desc = descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
        // replace(",", "") để chấp nhận format "1,000,000"
        String priceText = startPriceField.getText().trim().replace(",", "");
        String durationText = durationField.getText().trim();

        // Validate field bắt buộc
        if (name.isEmpty() || priceText.isEmpty() || durationText.isEmpty()) {
            showError("Vui lòng điền đầy đủ thông tin bắt buộc");
            return;
        }

        try {
            // Parse số
            double price = Double.parseDouble(priceText);
            int durationMinutes = Integer.parseInt(durationText);

            // Validate số dương
            if (price <= 0) {
                showError("Giá khởi điểm phải lớn hơn 0");
                return;
            }
            if (durationMinutes <= 0) {
                showError("Thời lượng phải lớn hơn 0 phút");
                return;
            }

            // Strategy trả về category enum + extra data — không cần if-else
            ItemCategory category = currentStrategy.getCategory();
            Map<String, String> extra = currentStrategy.collectData();

            // ===== 2 ROUND-TRIPS LÊN SERVER =====
            // 1. Tạo Item → lấy itemId
            String itemId = auctionClientService.createItem(category.name(), name, desc, price, extra);
            // 2. Tạo Auction với itemId vừa có
            auctionClientService.createAuction(itemId, name, price, durationMinutes);

            // Thành công → quay về Dashboard
            MainApp.navigateTo("/com/auction/view/dashboard.fxml", "Trang chủ");

        } catch (NumberFormatException e) {
            // Parse double/int thất bại
            showError("Giá hoặc thời lượng không hợp lệ");
        } catch (IOException e) {
            // Lỗi mạng hoặc lỗi server
            showError("Lỗi: " + e.getMessage());
        }
    }

    /** Nút "Quay lại" - về Dashboard. */
    @FXML
    private void handleBack() {
        MainApp.navigateTo("/com/auction/view/dashboard.fxml", "Trang chủ");
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
