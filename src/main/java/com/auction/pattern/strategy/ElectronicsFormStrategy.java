package com.auction.pattern.strategy;

import com.auction.model.item.ItemCategory;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy tạo form cho danh mục Điện tử.
 * Các trường: hãng sản xuất (brand), model, tình trạng (condition).
 */
public class ElectronicsFormStrategy implements CategoryFormStrategy {

    private TextField brandField;
    private TextField modelField;
    private TextField conditionField;

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.ELECTRONICS;
    }

    @Override
    public void buildFields(VBox container) {
        brandField = FormFieldHelper.addField(container, "Hãng sản xuất", "Ví dụ: Apple, Samsung");
        modelField = FormFieldHelper.addField(container, "Model", "Ví dụ: iPhone 15 Pro");
        conditionField = FormFieldHelper.addField(container, "Tình trạng", "NEW / LIKE_NEW / USED");
    }

    @Override
    public Map<String, String> collectData() {
        Map<String, String> data = new HashMap<>();
        if (brandField != null) data.put("brand", brandField.getText().trim());
        if (modelField != null) data.put("model", modelField.getText().trim());
        if (conditionField != null) data.put("condition", conditionField.getText().trim());
        return data;
    }
}
