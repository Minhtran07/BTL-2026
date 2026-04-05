package com.auction.model.user;
/**
 * Enum định nghĩa các vai trò người dùng trong hệ thống.
 */
public enum UserRole {
    BIDDER("Bidder"),
    SELLER("Seller"),
    ADMIN("Admin");

    private final String displayName;
    UserRole(String displayName){
        this.displayName = displayName;
    }
    public String getDisplayName(){
        return displayName;
    }

}
