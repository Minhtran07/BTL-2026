package com.auction.controller;

import com.auction.MainApp;
import com.auction.SessionManager;
import com.auction.model.item.ItemCategory;
import com.auction.model.user.User;
import com.auction.network.AuctionClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller cho màn hình tạo phiên đấu giá mới — gọi qua server.
 */
public class CreateAuctionController {

    @FXML private TextField itemNameField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextArea descriptionArea;
    @FXML private TextField startPriceField;
    @FXML private TextField durationField;
    @FXML private VBox extraFieldsContainer;
    @FXML private Label errorLabel;

    private final AuctionClient client = AuctionClient.getInstance();

    private TextField brandField;
    private TextField modelField;
    private TextField conditionField;
    private TextField artistField;
    private TextField yearField;
    private TextField mediumField;
    private TextField makeField;
    private TextField vehicleModelField;
    private TextField mileageField;

    @FXML
    private void initialize() {
        categoryComboBox.getItems().addAll("Điện tử", "Nghệ thuật", "Phương tiện");
        categoryComboBox.getSelectionModel().selectFirst();
        categoryComboBox.setOnAction(e -> updateExtraFields());
        updateExtraFields();
    }

    private void updateExtraFields() {
        extraFieldsContainer.getChildren().clear();
        String category = categoryComboBox.getValue();

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

    private TextField addExtraField(String label, String prompt) {
        VBox box = new VBox(4);
        Label lbl = new Label(label);
        TextField field = new TextField();
        field.setPromptText(prompt);
        box.getChildren().addAll(lbl, field);
        extraFieldsContainer.getChildren().add(box);
        return field;
    }

    @FXML
    private void handleCreate() {
        hideError();
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        String name = itemNameField.getText().trim();
        String desc = descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
        String priceText = startPriceField.getText().trim().replace(",", "");
        String durationText = durationField.getText().trim();

        if (name.isEmpty() || priceText.isEmpty() || durationText.isEmpty()) {
            showError("Vui lòng điền đầy đủ thông tin bắt buộc");
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            int durationMinutes = Integer.parseInt(durationText);

            if (price <= 0) {
                showError("Giá khởi điểm phải lớn hơn 0");
                return;
            }
            if (durationMinutes <= 0) {
                showError("Thời lượng phải lớn hơn 0 phút");
                return;
            }

            ItemCategory category;
            String catStr = categoryComboBox.getValue();
            if ("Điện tử".equals(catStr)) category = ItemCategory.ELECTRONICS;
            else if ("Nghệ thuật".equals(catStr)) category = ItemCategory.ART;
            else category = ItemCategory.VEHICLE;

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

            // 2 round trips: createItem rồi createAuction
            String itemId = client.createItem(category.name(), name, desc, price, extra);
            client.createAuction(itemId, name, price, durationMinutes);

            MainApp.navigateTo("/com/auction/view/dashboard.fxml", "Trang chủ");

        } catch (NumberFormatException e) {
            showError("Giá hoặc thời lượng không hợp lệ");
        } catch (IOException e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

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
