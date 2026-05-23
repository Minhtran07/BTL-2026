package com.auction.model.item;

import com.auction.model.entity.Entity;

/**
 * Lớp trừu tượng Item - sản phẩm đấu giá.
 * Kế thừa Entity, là lớp cha cho Electronics, Art, Vehicle.
 */
public abstract class Item extends Entity {

    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private double startingPrice;
    private String sellerId;
    private String imageUrl;

    protected Item() {
        super();
    }

    protected Item(String name, String description, double startingPrice, String sellerId) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerId = sellerId;
    }

    /**
     * Phương thức trừu tượng trả về danh mục sản phẩm - Polymorphism.
     */
    public abstract ItemCategory getCategory();

    // Encapsulation
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        markUpdated();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        markUpdated();
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
        markUpdated();
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        markUpdated();
    }

    @Override
    public String printInfo() {
        return String.format("Item[id=%s, name=%s, category=%s, startPrice=%.2f]",
                getId(), name, getCategory(), startingPrice);
    }
}