package com.auction.network.message.request;

import com.auction.network.message.Request;
import java.util.Map;

public class CreateItemRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String category;
    private final String name;
    private final String description;
    private final double price;

    public CreateItemRequest(String category, String name, String description,
                             double price, Map<String, String> extra) {
        super(Type.CREATE_ITEM);
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        put("category", category);
        put("name", name);
        put("description", description);
        put("price", String.valueOf(price));
        if (extra != null) {
            extra.forEach(this::put);
        }
    }

    public String getCategory() { return category; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
}
