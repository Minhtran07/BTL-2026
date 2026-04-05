package com.auction.model.item;

import com.auction.model.entity.Entity;

public abstract class Item extends Entity{

    private static final long serialVersionUID = 1L;
    private String name;
    private String description;
    private double startingPrice;
    private String sellerId;
    private String imageUrl;

    protected Item(){
        super();
    }

    protected Item(String name, String description, double startingPrice, String sellerId){
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerId = sellerId;
    }
    // phương thức trừu tượng trả về danh sách các sản phẩm
    public abstract ItemCategory getCategory();

    // tính đóng gói vs các thuộc tiính private


    public void setName(String name) {
        this.name = name;
        markUpdated();
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
        markUpdated();
    }

    public String getDescription() {
        return description;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
        markUpdated();
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
        markUpdated();
    }

    public String getSellerId() {
        return sellerId;
    }
    public void setImageUrl(){
        this.imageUrl = imageUrl;
        markUpdated();
    }
    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public String printInfo(){
        return String.format("Item[id=%s, name=%s, category=%s, startPrice=%.2f]",
                getId(),name,getCategory(),startingPrice);
    }
}
