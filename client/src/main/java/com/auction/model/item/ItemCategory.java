package com.auction.model.item;
/**
 * Enum danh mục sản phẩm.
 */

public enum ItemCategory   {
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
