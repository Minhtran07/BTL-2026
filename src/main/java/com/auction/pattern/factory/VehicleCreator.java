package com.auction.pattern.factory;

import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;

import java.util.Map;

/**
 * Creator cho phương tiện.
 * Chỉ chịu trách nhiệm khởi tạo {@link Vehicle} – tuân thủ SRP.
 */
public class VehicleCreator implements ItemCreator {

    @Override
    public Item create(String name, String desc, double price,
                       String sellerId, Map<String, String> extra) {
        String make         = extra.getOrDefault("make",          "Unknown");
        String vehicleModel = extra.getOrDefault("vehicleModel",  "Unknown");
        int    year         = parseIntOrDefault(extra.get("year"),    2024);
        int    mileage      = parseIntOrDefault(extra.get("mileage"),    0);
        return new Vehicle(name, desc, price, sellerId, make, vehicleModel, year, mileage);
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
