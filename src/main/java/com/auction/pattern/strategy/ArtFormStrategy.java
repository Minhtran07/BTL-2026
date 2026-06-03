package com.auction.pattern.strategy;

import com.auction.model.item.ItemCategory;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy tạo form cho danh mục Nghệ thuật.
 * Các trường: nghệ sĩ (artist), năm sáng tác (year), chất liệu (medium).
 */
public class ArtFormStrategy implements CategoryFormStrategy {

    private TextField artistField;
    private TextField yearField;
    private TextField mediumField;

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.ART;
    }

    @Override
    public void buildFields(VBox container) {
        artistField = FormFieldHelper.addField(container, "Nghệ sĩ", "Tên tác giả");
        yearField = FormFieldHelper.addField(container, "Năm sáng tác", "Ví dụ: 2024");
        mediumField = FormFieldHelper.addField(container, "Chất liệu", "Oil / Watercolor / Digital");
    }

    @Override
    public Map<String, String> collectData() {
        Map<String, String> data = new HashMap<>();
        if (artistField != null) data.put("artist", artistField.getText().trim());
        if (yearField != null) data.put("year", yearField.getText().trim());
        if (mediumField != null) data.put("medium", mediumField.getText().trim());
        return data;
    }
}
