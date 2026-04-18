package com.auction.pattern.factory;

import com.auction.model.item.Electronics;
import com.auction.model.item.Item;

import java.util.Map;

/**
 * Creator cho sản phẩm điện tử.
 * Chỉ chịu trách nhiệm khởi tạo {@link Electronics} – tuân thủ SRP.
 */
public class  ElectronicsCreator implements ItemCreator {

    @Override
    public Item create(String name, String desc, double price,
                       String sellerId, Map<String, String> extra) {
        String brand     = extra.getOrDefault("brand",     "Unknown");
        String model     = extra.getOrDefault("model",     "Unknown");
        String condition = extra.getOrDefault("condition", "USED");
        return new Electronics(name, desc, price, sellerId, brand, model, condition);
    }
}
