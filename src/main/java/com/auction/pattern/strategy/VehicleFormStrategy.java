package com.auction.pattern.strategy;

import com.auction.model.item.ItemCategory;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy tạo form cho danh mục Phương tiện.
 * Các trường: hãng xe (make), model xe, năm sản xuất (year), số km (mileage).
 */
public class VehicleFormStrategy implements CategoryFormStrategy {

    private TextField makeField;
    private TextField vehicleModelField;
    private TextField yearField;
    private TextField mileageField;

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.VEHICLE;
    }

    @Override
    public void buildFields(VBox container) {
        makeField = FormFieldHelper.addField(container, "Hãng xe", "Ví dụ: Toyota, Honda");
        vehicleModelField = FormFieldHelper.addField(container, "Model xe", "Ví dụ: Camry 2.5Q");
        yearField = FormFieldHelper.addField(container, "Năm sản xuất", "Ví dụ: 2023");
        mileageField = FormFieldHelper.addField(container, "Số km đã đi", "Ví dụ: 15000");
    }

    @Override
    public Map<String, String> collectData() {
        Map<String, String> data = new HashMap<>();
        if (makeField != null) data.put("make", makeField.getText().trim());
        if (vehicleModelField != null) data.put("vehicleModel", vehicleModelField.getText().trim());
        if (yearField != null) data.put("year", yearField.getText().trim());
        if (mileageField != null) data.put("mileage", mileageField.getText().trim());
        return data;
    }
}
