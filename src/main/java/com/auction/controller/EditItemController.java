package com.auction.controller;

import com.auction.MainApp;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;
import com.auction.network.AuctionClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Controller chỉnh sửa / xoá sản phẩm — qua server.
 */
public class EditItemController {

    @FXML private TextField itemNameField;
    @FXML private TextField categoryField;
    @FXML private TextArea  descriptionArea;
    @FXML private TextField startPriceField;
    @FXML private VBox      extraFieldsContainer;
    @FXML private Label     errorLabel;
    @FXML private Label     successLabel;

    private final AuctionClient client = AuctionClient.getInstance();

    private Item currentItem;

    private TextField brandField;
    private TextField modelField;
    private TextField conditionField;
    private TextField artistField;
    private TextField yearField;
    private TextField mediumField;
    private TextField makeField;
    private TextField vehicleModelField;
    private TextField mileageField;

    public void setItem(Item item) {
        this.currentItem = item;
        populateFields();
    }

    @FXML
    private void handleSave() {
        hideMessages();

        if (currentItem == null) {
            showError("Không tìm thấy sản phẩm.");
            return;
        }

        String name      = itemNameField.getText().trim();
        String desc      = descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
        String priceText = startPriceField.getText().trim().replace(",", "");

        if (name.isEmpty() || priceText.isEmpty()) {
            showError("Vui lòng điền đầy đủ tên và giá khởi điểm.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            showError("Giá khởi điểm không hợp lệ.");
            return;
        }
        if (price <= 0) {
            showError("Giá khởi điểm phải lớn hơn 0.");
            return;
        }

        currentItem.setName(name);
        currentItem.setDescription(desc);
        currentItem.setStartingPrice(price);

        if (currentItem instanceof Electronics elec) {
            if (brandField   != null) elec.setBrand(brandField.getText().trim());
            if (modelField   != null) elec.setModel(modelField.getText().trim());
            if (conditionField != null) elec.setCondition(conditionField.getText().trim());
        } else if (currentItem instanceof Art art) {
            if (artistField != null) art.setArtist(artistField.getText().trim());
            if (mediumField != null) art.setMedium(mediumField.getText().trim());
            if (yearField   != null) {
                try { art.setYear(Integer.parseInt(yearField.getText().trim())); }
                catch (NumberFormatException ignored) {}
            }
        } else if (currentItem instanceof Vehicle v) {
            if (makeField         != null) v.setMake(makeField.getText().trim());
            if (vehicleModelField != null) v.setVehicleModel(vehicleModelField.getText().trim());
            if (mileageField      != null) {
                try { v.setMileage(Integer.parseInt(mileageField.getText().trim())); }
                catch (NumberFormatException ignored) {}
            }
            if (yearField != null) {
                try { v.setYear(Integer.parseInt(yearField.getText().trim())); }
                catch (NumberFormatException ignored) {}
            }
        }

        try {
            client.updateItem(currentItem);
            showSuccess("Đã lưu thay đổi thành công!");
        } catch (IOException e) {
            showError("Lỗi khi lưu: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (currentItem == null) return;

        // Server cũng tự kiểm, nhưng kiểm trên client để show message thân thiện
        try {
            List<Auction> allAuctions = client.getAllAuctions();
            Optional<Auction> activeAuction = allAuctions.stream()
                    .filter(a -> a.getItemId().equals(currentItem.getId())
                            && (a.getStatus() == AuctionStatus.RUNNING
                                || a.getStatus() == AuctionStatus.OPEN))
                    .findFirst();

            if (activeAuction.isPresent()) {
                showError("Không thể xóa — phiên đấu giá '"
                        + activeAuction.get().getItemName()
                        + "' đang diễn ra hoặc chưa bắt đầu.");
                return;
            }
        } catch (IOException e) {
            showError("Lỗi kiểm tra phiên đấu giá: " + e.getMessage());
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa sản phẩm: " + currentItem.getName());
        confirm.setContentText("Bạn có chắc chắn muốn xóa sản phẩm này?\nHành động này không thể hoàn tác.");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) return;

        try {
            client.deleteItem(currentItem.getId());
            MainApp.navigateTo("/com/auction/view/dashboard.fxml", "Trang chủ");
        } catch (IOException e) {
            showError("Lỗi khi xóa: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        MainApp.navigateTo("/com/auction/view/dashboard.fxml", "Trang chủ");
    }

    private void populateFields() {
        if (currentItem == null) return;

        itemNameField.setText(currentItem.getName());
        descriptionArea.setText(currentItem.getDescription() != null ? currentItem.getDescription() : "");
        startPriceField.setText(String.valueOf((long) currentItem.getStartingPrice()));

        extraFieldsContainer.getChildren().clear();
        brandField = modelField = conditionField = null;
        artistField = yearField = mediumField = null;
        makeField = vehicleModelField = mileageField = null;

        if (currentItem instanceof Electronics elec) {
            categoryField.setText("Điện tử");
            brandField      = addExtraField("Hãng sản xuất", elec.getBrand());
            modelField      = addExtraField("Model",         elec.getModel());
            conditionField  = addExtraField("Tình trạng",    elec.getCondition());

        } else if (currentItem instanceof Art art) {
            categoryField.setText("Nghệ thuật");
            artistField = addExtraField("Nghệ sĩ",      art.getArtist());
            yearField   = addExtraField("Năm sáng tác",  String.valueOf(art.getYear()));
            mediumField = addExtraField("Chất liệu",     art.getMedium());

        } else if (currentItem instanceof Vehicle v) {
            categoryField.setText("Phương tiện");
            makeField         = addExtraField("Hãng xe",        v.getMake());
            vehicleModelField = addExtraField("Model xe",        v.getVehicleModel());
            yearField         = addExtraField("Năm sản xuất",   String.valueOf(v.getYear()));
            mileageField      = addExtraField("Số km đã đi",    String.valueOf(v.getMileage()));

        } else {
            categoryField.setText(currentItem.getCategory().name());
        }
    }

    private TextField addExtraField(String label, String value) {
        VBox box = new VBox(4);
        Label lbl   = new Label(label);
        TextField tf = new TextField();
        if (value != null) tf.setText(value);
        box.getChildren().addAll(lbl, tf);
        extraFieldsContainer.getChildren().add(box);
        return tf;
    }

    private void hideMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    private void showError(String msg) {
        errorLabel.setText("⚠ " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        successLabel.setText("✓ " + msg);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }
}
