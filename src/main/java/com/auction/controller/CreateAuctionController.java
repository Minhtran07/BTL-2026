package com.auction.controller;

import com.auction.MainApp;
import com.auction.SessionManager;
import com.auction.model.item.ItemCategory;
import com.auction.model.user.User;
import com.auction.network.client.AuctionClientService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.HashMap;
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

    // ===== Các field ĐỘNG (tạo runtime, có hoặc không tùy category) =====
    // Electronics
    private TextField brandField;
    private TextField modelField;
    private TextField conditionField;
    // Art
    private TextField artistField;
    private TextField yearField; // dùng chung cho Art & Vehicle
    private TextField mediumField;
    // Vehicle
    private TextField makeField;
    private TextField vehicleModelField;
    private TextField mileageField;

    /**
     * Initialize - chạy sau khi FXML load.
     * Setup ComboBox category + listener thay đổi field theo lựa chọn.
     */
    @FXML
    private void initialize() {
        // Thêm 3 loại sản phẩm vào dropdown
        categoryComboBox.getItems().addAll("Điện tử", "Nghệ thuật", "Phương tiện");
        categoryComboBox.getSelectionModel().selectFirst();
        // Mỗi khi đổi lựa chọn → cập nhật các field động
        categoryComboBox.setOnAction(e -> updateExtraFields());
        updateExtraFields(); // hiển thị field cho category mặc định
    }

    /**
     * Cập nhật các field động theo category đã chọn.
     * Xóa hết field cũ → thêm field mới phù hợp với category.
     */
    private void updateExtraFields() {
        extraFieldsContainer.getChildren().clear(); // xóa tất cả field cũ
        String category = categoryComboBox.getValue();

        // Tạo field phù hợp với từng category
        if ("Điện tử".equals(category)) {
            brandField = addExtraField("Hãng sản xuất", "Ví dụ: Apple, Samsung");
            modelField = addExtraField("Model", "Ví dụ: iPhone 15 Pro");
            conditionField = addExtraField("Tình trạng", "NEW / LIKE_NEW / USED");
        } else if ("Nghệ thuật".equals(category)) {
            artistField = addExtraField("Nghệ sĩ", "Tên tác giả");
            yearField = addExtraField("Năm sáng tác", "Ví dụ: 2024");
            mediumField = addExtraField("Chất liệu", "Oil / Watercolor / Digital");
        } else if ("Phương tiện".equals(category)) {
            makeField = addExtraField("Hãng xe", "Ví dụ: Toyota, Honda");
            vehicleModelField = addExtraField("Model xe", "Ví dụ: Camry 2.5Q");
            yearField = addExtraField("Năm sản xuất", "Ví dụ: 2023");
            mileageField = addExtraField("Số km đã đi", "Ví dụ: 15000");
        }
    }

    /**
     * Tạo 1 label + textfield và thêm vào extraFieldsContainer.
     * Trả về TextField để controller giữ tham chiếu.
     */
    private TextField addExtraField(String label, String prompt) {
        VBox box = new VBox(4); // box dọc với spacing 4px
        Label lbl = new Label(label);
        TextField field = new TextField();
        field.setPromptText(prompt); // text placeholder
        box.getChildren().addAll(lbl, field);
        extraFieldsContainer.getChildren().add(box);
        return field;
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

            // Chuyển String → enum ItemCategory
            ItemCategory category;
            String catStr = categoryComboBox.getValue();
            if ("Điện tử".equals(catStr)) category = ItemCategory.ELECTRONICS;
            else if ("Nghệ thuật".equals(catStr)) category = ItemCategory.ART;
            else category = ItemCategory.VEHICLE;

            // Build map extra fields theo category
            Map<String, String> extra = new HashMap<>();
            if (category == ItemCategory.ELECTRONICS) {
                if (brandField != null) extra.put("brand", brandField.getText().trim());
                if (modelField != null) extra.put("model", modelField.getText().trim());
                if (conditionField != null) extra.put("condition", conditionField.getText().trim());
            } else if (category == ItemCategory.ART) {
                if (artistField != null) extra.put("artist", artistField.getText().trim());
                if (yearField != null) extra.put("year", yearField.getText().trim());
                if (mediumField != null) extra.put("medium", mediumField.getText().trim());
            } else {
                if (makeField != null) extra.put("make", makeField.getText().trim());
                if (vehicleModelField != null) extra.put("vehicleModel", vehicleModelField.getText().trim());
                if (yearField != null) extra.put("year", yearField.getText().trim());
                if (mileageField != null) extra.put("mileage", mileageField.getText().trim());
            }

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
