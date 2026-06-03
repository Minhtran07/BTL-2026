package com.auction.pattern.strategy;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Utility class hỗ trợ tạo field trên form.
 *
 * <p>Tách riêng khỏi {@link CategoryFormStrategy} (SRP) — strategy interface
 * chỉ định nghĩa contract, không chứa logic tạo UI.
 */
public final class FormFieldHelper {

    private FormFieldHelper() {
        // Utility class — không cho tạo instance
    }

    /**
     * Tạo 1 label + textfield, thêm vào container và trả về TextField.
     *
     * @param container VBox chứa các field
     * @param labelText tên hiển thị của field
     * @param prompt    placeholder text
     * @return TextField đã tạo (để strategy giữ tham chiếu)
     */
    public static TextField addField(VBox container, String labelText, String prompt) {
        VBox box = new VBox(4);
        Label lbl = new Label(labelText);
        TextField field = new TextField();
        field.setPromptText(prompt);
        box.getChildren().addAll(lbl, field);
        container.getChildren().add(box);
        return field;
    }
}
