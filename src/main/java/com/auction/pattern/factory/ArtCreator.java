package com.auction.pattern.factory;

import com.auction.model.item.Art;
import com.auction.model.item.Item;

import java.util.Map;

/**
 * Creator cho sản phẩm nghệ thuật.
 * Chỉ chịu trách nhiệm khởi tạo {@link Art} – tuân thủ SRP.
 */
public class ArtCreator implements ItemCreator {

    @Override
    public Item create(String name, String desc, double price,
                       String sellerId, Map<String, String> extra) {
        String artist = extra.getOrDefault("artist", "Unknown");
        int    year   = parseIntOrDefault(extra.get("year"), 2024);
        String medium = extra.getOrDefault("medium", "Mixed");
        return new Art(name, desc, price, sellerId, artist, year, medium);
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
