package com.auction.network.message.request;

import java.util.HashMap;
import java.util.Map;

/**
 * Request tạo sản phẩm mới — chứa category, name, description, price, extraFields.
 * Handler: {@link com.auction.network.handler.CreateItemHandler}
 * <p>{@code extraFields} chứa field riêng của subtype (brand, artist, mileage...).
 */
public class CreateItemRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String category;
    private final String name;
    private final String description;
    private final double price;
    /** Các field riêng của subtype (brand, model, artist, mileage...). */
    private final HashMap<String, String> extraFields;

    public CreateItemRequest(String category, String name, String description,
                             double price, Map<String, String> extra) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        this.extraFields = extra != null ? new HashMap<>(extra) : new HashMap<>();
    }

    public String getCategory() { return category; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public Map<String, String> getExtraFields() { return extraFields; }
}
