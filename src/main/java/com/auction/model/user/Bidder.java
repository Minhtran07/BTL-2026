package com.auction.model.user;



import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Bidder - người tham gia đấu giá.
 * Kế thừa User, override getRole() - Polymorphism.
 */
public class Bidder extends User {

    private static final long serialVersionUID = 1L;

    private double balance;
    private final List<String> wonAuctionIds;
    private final List<String> participatingAuctionIds;

    public Bidder() {
        super();
        this.balance = 0.0;
        this.wonAuctionIds = new ArrayList<>();
        this.participatingAuctionIds = new ArrayList<>();
    }

    public Bidder(String username, String password, String email, String fullName) {
        super(username, password, email, fullName);
        this.balance = 10000.0; // Default balance
        this.wonAuctionIds = new ArrayList<>();
        this.participatingAuctionIds = new ArrayList<>();
    }

    @Override
    public UserRole getRole() {
        return UserRole.BIDDER;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
        markUpdated();
    }

    public void addBalance(double amount) {
        this.balance += amount;
        markUpdated();
    }

    public boolean deductBalance(double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            markUpdated();
            return true;
        }
        return false;
    }

    public List<String> getWonAuctionIds() {
        return new ArrayList<>(wonAuctionIds);
    }

    public void addWonAuction(String auctionId) {
        wonAuctionIds.add(auctionId);
        markUpdated();
    }

    public List<String> getParticipatingAuctionIds() {
        return new ArrayList<>(participatingAuctionIds);
    }

    public void addParticipatingAuction(String auctionId) {
        if (!participatingAuctionIds.contains(auctionId)) {
            participatingAuctionIds.add(auctionId);
            markUpdated();
        }
    }

    @Override
    public String printInfo() {
        return String.format("Bidder[id=%s, username=%s, balance=%.2f, wonAuctions=%d]",
                getId(), getUsername(), balance, wonAuctionIds.size());
    }
}