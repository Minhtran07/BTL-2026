package com.auction.controller;

import com.auction.MainApp;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;
import com.auction.network.client.AuctionClientService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * EDITITEMCONTROLLER - CONTROLLER CHỈNH SỬA / XÓA ITEM
 * ============================================================================
 *
 * <p>Cho phép Seller sửa thông tin sản phẩm hoặc xóa hẳn. Tính năng:
 * <ul>
 *   <li>Load thông tin Item hiện tại lên form (populateFields)</li>
 *   <li>Field động theo loại Item (Electronics/Art/Vehicle)</li>
 *   <li>Lưu thay đổi qua server</li>
 *   <li>Xóa Item với confirm dialog + kiểm tra không có auction đang chạy</li>
 * </ul>
 *
 * <p><b>QUAN TRỌNG:</b> Không xóa Item nếu có Auction đang chạy/chưa bắt đầu
 * - tránh phá vỡ tham chiếu trong DB.
 */
public class EditItemController {

    // ===== UI Elements =====
    @FXML private TextField itemNameField;
    @FXML private TextField categoryField; // hiển thị category, không cho sửa
    @FXML private TextArea  descriptionArea;
    @FXML private TextField startPriceField;
    @FXML private VBox      extraFieldsContainer;
    @FXML private Label     errorLabel;
    @FXML private Label     successLabel;

    private final AuctionClientService auctionClientService = AuctionClientService.getInstance();

    /** Item đang được edit - DashboardController set vào trước khi navigate. */
    private Item currentItem;

    // ===== Các field động (tạo runtime tùy loại Item) =====
    private TextField brandField;
    private TextField modelField;
    private TextField conditionField;
    private TextField artistField;
    private TextField yearField;
    private TextField mediumField;
    private TextField makeField;
    private TextField vehicleModelField;
    private TextField mileageField;

    /**
     * Phương thức public để DashboardController truyền Item vào trước khi
     * hiển thị màn hình edit. Gọi populateFields() để hiển thị data lên UI.
     */
    public void setItem(Item item) {
        this.currentItem = item;
        populateFields();
    }

    /**
     * Xử lý nút "Lưu" - validate + cập nhật Item.
     */
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

        // Parse giá
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

        // ===== Cập nhật field chung =====
        currentItem.setName(name);
        currentItem.setDescription(desc);
        currentItem.setStartingPrice(price);

        // ===== Cập nhật field riêng theo loại Item (pattern matching) =====
        if (currentItem instanceof Electronics elec) {
            if (brandField   != null) elec.setBrand(brandField.getText().trim());
            if (modelField   != null) elec.setModel(modelField.getText().trim());
            if (conditionField != null) elec.setCondition(conditionField.getText().trim());
        } else if (currentItem instanceof Art art) {
            if (artistField != null) art.setArtist(artistField.getText().trim());
            if (mediumField != null) art.setMedium(mediumField.getText().trim());
            if (yearField   != null) {
                // Try-catch riêng cho parseInt - tránh fail tổng thể nếu year sai
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

        // Gửi update lên server
        try {
            auctionClientService.updateItem(currentItem);
            showSuccess("Đã lưu thay đổi thành công!");
        } catch (IOException e) {
            showError("Lỗi khi lưu: " + e.getMessage());
        }
    }

    /**
     * Xử lý nút "Xóa" - kiểm tra ràng buộc + confirm + xóa.
     *
     * <p><b>Quy trình:</b>
     * <ol>
     *   <li>Kiểm tra có Auction nào đang RUNNING hoặc OPEN với item này không
     *       → có thì từ chối xóa</li>
     *   <li>Hiển thị dialog xác nhận</li>
     *   <li>Nếu YES → gọi server xóa</li>
     * </ol>
     */
    @FXML
    private void handleDelete() {
        if (currentItem == null) return;

        // ===== Kiểm tra ràng buộc =====
        // Server cũng kiểm, nhưng kiểm trên client để show message thân thiện sớm hơn
        try {
            List<Auction> allAuctions = auctionClientService.getAllAuctions();
            // Tìm xem có auction nào đang dùng item này KHÔNG ở trạng thái kết thúc
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

        // ===== Dialog xác nhận =====
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa sản phẩm: " + currentItem.getName());
        confirm.setContentText("Bạn có chắc chắn muốn xóa sản phẩm này?\nHành động này không thể hoàn tác.");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        // showAndWait() đợi user phản hồi, trả về Optional<ButtonType>
        Optional<ButtonType> result = confirm.showAndWait();
        // User đóng dialog (Optional empty) hoặc chọn NO → hủy
        if (result.isEmpty() || result.get() != ButtonType.YES) return;

        // ===== Xóa =====
        try {
            auctionClientService.deleteItem(currentItem.getId());
            MainApp.navigateTo("/com/auction/view/dashboard.fxml", "Trang chủ");
        } catch (IOException e) {
            showError("Lỗi khi xóa: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        MainApp.navigateTo("/com/auction/view/dashboard.fxml", "Trang chủ");
    }

    /**
     * Đổ data của currentItem lên form.
     * Tùy loại Item → tạo field động phù hợp.
     */
    private void populateFields() {
        if (currentItem == null) return;

        // Field chung
        itemNameField.setText(currentItem.getName());
        descriptionArea.setText(currentItem.getDescription() != null ? currentItem.getDescription() : "");
        // Cast double → long để bỏ phần thập phân khi hiển thị (vd: 1000000 thay vì 1000000.0)
        startPriceField.setText(String.valueOf((long) currentItem.getStartingPrice()));

        // Clear field cũ trước khi tạo mới
        extraFieldsContainer.getChildren().clear();
        // Reset tất cả tham chiếu - tránh dùng nhầm field cũ
        brandField = modelField = conditionField = null;
        artistField = yearField = mediumField = null;
        makeField = vehicleModelField = mileageField = null;

        // Tạo field động + đổ data theo loại Item
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
            // Loại không xác định → chỉ hiển thị category, không có field riêng
            categoryField.setText(currentItem.getCategory().name());
        }
    }

    /** Thêm 1 label + textfield đã điền sẵn value vào container động. */
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
