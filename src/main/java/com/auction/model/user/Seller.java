package com.auction.model.user;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Seller - người bán, đăng sản phẩm đấu giá.
 * Kế thừa User - Polymorphism.
 */
public class Seller extends User {

    private static final long serialVersionUID = 1L;

    private final List<String> listedItemIds;
    private double totalRevenue;

    public Seller() {
        super();
        this.listedItemIds = new ArrayList<>();
        this.totalRevenue = 0.0;
    }

    public Seller(String username, String password, String email, String fullName) {
        super(username, password, email, fullName);
        this.listedItemIds = new ArrayList<>();
        this.totalRevenue = 0.0;
    }

    @Override
    public UserRole getRole() {
        return UserRole.SELLER;
    }

    public List<String> getListedItemIds() {
        return new ArrayList<>(listedItemIds);
    }

    public void addListedItem(String itemId) {
        listedItemIds.add(itemId);
        markUpdated();
    }

    public void removeListedItem(String itemId) {
        listedItemIds.remove(itemId);
        markUpdated();
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void addRevenue(double amount) {
        this.totalRevenue += amount;
        markUpdated();
    }

    @Override
    public String printInfo() {
        return String.format("Seller[id=%s, username=%s, items=%d, revenue=%.2f]",
                getId(), getUsername(), listedItemIds.size(), totalRevenue);
    }
}