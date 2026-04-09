package com.auction.model.item;


// enum cho danh mục các sản phẩm hiện có của sàn
public enum ItemCategory {
    ELECTRONICS("Điện tử"),
    ART("Nghệ thuật"),
    VEHICLE("Phương tiện"),
    OTHER("Khác");

    private final String displayName;

    ItemCategory(String displayName){
        this.displayName = displayName;
    }

    public String getDisplayName(){
        return displayName;
    }
}
